'use client'

import { Fragment, useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { PreviewCard } from '@base-ui/react/preview-card'
import {
  ArrowDown,
  ArrowRightToLine,
  ArrowUp,
  ArrowUpDown,
  ChevronDown,
  ChevronRight,
  Loader2,
  Columns3,
  RotateCw,
  Trash2,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { cn } from '@/lib/utils'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { PageResponse } from './types'

export type Column<T> = {
  key: string
  label: string
  value: (row: T) => string
  render?: (row: T) => React.ReactNode
  cellClassName?: string
  headClassName?: string
  defaultWidth?: number
  skeleton?: string
  /** Backend field id this column maps to; when set, the column is offered as a search scope. */
  searchKey?: string
  sortKey?: string
  initialSortDirection?: 'asc' | 'desc'
  defaultVisible?: boolean
}

type DataTabProps<T> = {
  columns: Column<T>[]
  columnWidthsKey?: string
  rowKey: (row: T) => string
  load: (
    page: number,
    size: number,
    q?: string,
    field?: string,
    sort?: string,
    direction?: string,
  ) => Promise<PageResponse<T>>
  /** Resolves the page index where a searched row lives in the unfiltered list, enabling the jump action. */
  locate?: (row: T, size: number, direction?: string) => Promise<number>
  /** When set, rows get a chevron toggle that reveals this content in a full-width row below. */
  expand?: (row: T) => React.ReactNode
  /** Reads the room name off a row so it can be shown once instead of as a per-row column. */
  roomAccessor?: (row: T) => string
  rowActions?: (row: T) => React.ReactNode
  canEdit?: boolean
  onDelete?: (row: T) => Promise<unknown>
  emptyLabel: string
  /** Label for the column the rows are ordered by (shown on the sort toggle). Defaults to "time". */
  sortLabel?: string
  sortDescendingLabel?: string
  sortAscendingLabel?: string
  defaultSortKey?: string
  defaultSortDirection?: 'asc' | 'desc'
  forceLoading?: boolean
  columnSelection?: boolean
  active?: boolean
  refreshToken?: number
  onError: (message: string) => void
}

const PAGE_SIZES = [20, 50, 100]
const DEFAULT_PAGE_SIZE = 50
const SKELETON_ROWS = 10
const MIN_COLUMN_WIDTH = 64
const MAX_COLUMN_WIDTH = 640
const KEYBOARD_RESIZE_STEP = 16

type OverflowPreviewPayload = {
  getText: () => string
  isOverflowing: () => boolean
}

function hasOverflow(element: HTMLElement): boolean {
  if (element.scrollWidth > element.clientWidth || element.scrollHeight > element.clientHeight) return true
  const range = document.createRange()
  range.selectNodeContents(element)
  return [...range.getClientRects()].some((rect) => rect.width > element.clientWidth)
}

const SKELETON_BAR_WIDTHS = [
  'w-[85%]',
  'w-[58%]',
  'w-[72%]',
  'w-[46%]',
  'w-[90%]',
  'w-[64%]',
  'w-[78%]',
  'w-[52%]',
]

export function DataTab<T>({
  columns,
  columnWidthsKey,
  rowKey,
  load,
  locate,
  expand,
  roomAccessor,
  rowActions,
  canEdit = false,
  onDelete,
  emptyLabel,
  sortLabel = 'time',
  sortDescendingLabel = 'Newest',
  sortAscendingLabel = 'Oldest',
  defaultSortKey,
  defaultSortDirection = 'desc',
  forceLoading = false,
  columnSelection = false,
  active = true,
  refreshToken = 0,
  onError,
}: DataTabProps<T>) {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE)
  const [data, setData] = useState<PageResponse<T> | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [query, setQuery] = useState('')
  const [activeQuery, setActiveQuery] = useState('')
  const [field, setField] = useState('all')
  const [sortKey, setSortKey] = useState(() => defaultSortKey ?? columns.find((column) => column.sortKey)?.sortKey ?? '')
  const [direction, setDirection] = useState<'desc' | 'asc'>(defaultSortDirection)
  const [highlightId, setHighlightId] = useState<string | null>(null)
  const [jumpingId, setJumpingId] = useState<string | null>(null)
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [visibleColumnKeys, setVisibleColumnKeys] = useState<Set<string>>(
    () => new Set(columns.filter((column) => column.defaultVisible !== false).map((column) => column.key)),
  )
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({})
  const [hasCustomColumnWidths, setHasCustomColumnWidths] = useState(false)
  const [resizingColumn, setResizingColumn] = useState<string | null>(null)
  const [overflowPreviewHandle] = useState(() => PreviewCard.createHandle<OverflowPreviewPayload>())
  const tableRef = useRef<HTMLTableElement | null>(null)
  const columnWidthsRef = useRef<Record<string, number>>({})
  const defaultColumnWidthsRef = useRef<Record<string, number>>({})
  const customColumnKeysRef = useRef<Set<string>>(new Set())
  const resizeRef = useRef<{
    key: string
    nextKey: string
    pointerId: number
    startX: number
    startWidth: number
    startNextWidth: number
  } | null>(null)
  const highlightTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const columnWidthsStorageKey = `data-console.column-widths.v2.${columnWidthsKey ?? columns.map((column) => column.key).join('.')}`

  const persistColumnWidths = useCallback((widths: Record<string, number>) => {
    try {
      const customWidths = Object.fromEntries(
        [...customColumnKeysRef.current]
          .filter((key) => widths[key] !== undefined)
          .map((key) => [key, widths[key]]),
      )
      localStorage.setItem(columnWidthsStorageKey, JSON.stringify(customWidths))
    } catch {
      // Resizing remains available when storage is blocked or full.
    }
  }, [columnWidthsStorageKey])

  const updateColumnBoundary = useCallback((
    key: string,
    nextKey: string,
    startWidth: number,
    startNextWidth: number,
    requestedDelta: number,
    persist = false,
  ) => {
    const minimumDelta = Math.max(MIN_COLUMN_WIDTH - startWidth, startNextWidth - MAX_COLUMN_WIDTH)
    const maximumDelta = Math.min(MAX_COLUMN_WIDTH - startWidth, startNextWidth - MIN_COLUMN_WIDTH)
    const delta = Math.min(maximumDelta, Math.max(minimumDelta, requestedDelta))
    const width = Math.round(startWidth + delta)
    const nextWidth = startWidth + startNextWidth - width
    const next = { ...columnWidthsRef.current, [key]: width, [nextKey]: nextWidth }
    columnWidthsRef.current = next
    for (const changedKey of [key, nextKey]) {
      const defaultWidth = defaultColumnWidthsRef.current[changedKey]
      const changedWidth = next[changedKey]
      if (defaultWidth !== undefined && changedWidth !== undefined && Math.abs(changedWidth - defaultWidth) < 1) {
        customColumnKeysRef.current.delete(changedKey)
      } else {
        customColumnKeysRef.current.add(changedKey)
      }
    }
    setHasCustomColumnWidths(customColumnKeysRef.current.size > 0)
    setColumnWidths(next)
    if (persist) persistColumnWidths(next)
  }, [persistColumnWidths])

  const resetColumnBoundary = useCallback((key: string, nextKey: string) => {
    const defaultWidth = defaultColumnWidthsRef.current[key]
    const currentWidth = columnWidthsRef.current[key]
    const nextWidth = columnWidthsRef.current[nextKey]
    if (defaultWidth === undefined || currentWidth === undefined || nextWidth === undefined) return
    updateColumnBoundary(key, nextKey, currentWidth, nextWidth, defaultWidth - currentWidth, true)
  }, [updateColumnBoundary])

  const resetColumnWidths = useCallback(() => {
    const defaults = { ...defaultColumnWidthsRef.current }
    customColumnKeysRef.current.clear()
    columnWidthsRef.current = defaults
    setColumnWidths(defaults)
    setHasCustomColumnWidths(false)
    try {
      localStorage.removeItem(columnWidthsStorageKey)
    } catch {
      // The in-memory reset still applies when storage is unavailable.
    }
  }, [columnWidthsStorageKey])

  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem(columnWidthsStorageKey) ?? '{}') as Record<string, unknown>
      const validKeys = new Set(columns.map((column) => column.key))
      const restored = Object.fromEntries(
        Object.entries(stored).filter(
          ([key, width]) => validKeys.has(key) && typeof width === 'number' && Number.isFinite(width),
        ),
      ) as Record<string, number>
      customColumnKeysRef.current = new Set(Object.keys(restored))
      setHasCustomColumnWidths(customColumnKeysRef.current.size > 0)
      const merged = { ...columnWidthsRef.current, ...restored }
      columnWidthsRef.current = merged
      setColumnWidths(merged)
    } catch {
      try {
        localStorage.removeItem(columnWidthsStorageKey)
      } catch {
        // Ignore inaccessible storage and keep default widths.
      }
    }
  }, [columnWidthsStorageKey, columns])

  useLayoutEffect(() => {
    const headers = tableRef.current?.querySelectorAll<HTMLElement>('[data-column-key]')
    if (!headers?.length) return
    const next = { ...columnWidthsRef.current }
    let changed = false
    headers.forEach((header) => {
      const key = header.dataset.columnKey
      if (!key) return
      const measuredWidth = header.getBoundingClientRect().width
      if (defaultColumnWidthsRef.current[key] === undefined) {
        defaultColumnWidthsRef.current[key] = measuredWidth
      }
      if (next[key] === undefined) {
        next[key] = measuredWidth
        changed = true
      }
    })
    if (changed) {
      columnWidthsRef.current = next
      setColumnWidths(next)
    }
  }, [columns, visibleColumnKeys])

  useEffect(() => () => {
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }, [])

  const startColumnResize = (
    event: React.PointerEvent<HTMLDivElement>,
    key: string,
    nextKey: string,
  ) => {
    if (event.button !== 0) return
    event.preventDefault()
    event.stopPropagation()
    const header = event.currentTarget.parentElement
    if (!header) return
    const measuredWidths = { ...columnWidthsRef.current }
    tableRef.current?.querySelectorAll<HTMLElement>('[data-column-key]').forEach((columnHeader) => {
      const columnKey = columnHeader.dataset.columnKey
      if (columnKey) measuredWidths[columnKey] = columnHeader.getBoundingClientRect().width
    })
    const startWidth = measuredWidths[key] ?? header.getBoundingClientRect().width
    const startNextWidth = measuredWidths[nextKey]
    if (startNextWidth === undefined) return
    columnWidthsRef.current = measuredWidths
    setColumnWidths(measuredWidths)
    resizeRef.current = {
      key,
      nextKey,
      pointerId: event.pointerId,
      startX: event.clientX,
      startWidth,
      startNextWidth,
    }
    setResizingColumn(key)
    event.currentTarget.setPointerCapture(event.pointerId)
    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'
  }

  const resizeColumn = (event: React.PointerEvent<HTMLDivElement>) => {
    const resize = resizeRef.current
    if (!resize || resize.pointerId !== event.pointerId) return
    updateColumnBoundary(
      resize.key,
      resize.nextKey,
      resize.startWidth,
      resize.startNextWidth,
      event.clientX - resize.startX,
    )
  }

  const finishColumnResize = (event: React.PointerEvent<HTMLDivElement>) => {
    const resize = resizeRef.current
    if (!resize || resize.pointerId !== event.pointerId) return
    resizeRef.current = null
    setResizingColumn(null)
    persistColumnWidths(columnWidthsRef.current)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  const resizeColumnWithKeyboard = (
    event: React.KeyboardEvent<HTMLDivElement>,
    key: string,
    nextKey: string,
  ) => {
    if (event.key === 'Home') {
      event.preventDefault()
      resetColumnBoundary(key, nextKey)
      return
    }
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
    event.preventDefault()
    const currentWidth = columnWidthsRef.current[key]
      ?? event.currentTarget.parentElement?.getBoundingClientRect().width
      ?? MIN_COLUMN_WIDTH
    const nextWidth = columnWidthsRef.current[nextKey]
    if (nextWidth === undefined) return
    const direction = event.key === 'ArrowLeft' ? -1 : 1
    updateColumnBoundary(
      key,
      nextKey,
      currentWidth,
      nextWidth,
      direction * KEYBOARD_RESIZE_STEP,
      true,
    )
  }

  const reload = useCallback(
    async (
      targetPage: number,
      targetSize: number,
      q: string,
      searchField: string,
      targetSortKey: string,
      dir: string,
      // A silent reload keeps the current table visible and only spins the refresh icon,
      // instead of swapping the rows out for skeletons.
      opts?: { silent?: boolean },
    ) => {
      if (opts?.silent) setRefreshing(true)
      else setLoading(true)
      try {
        const result = await load(
          targetPage,
          targetSize,
          q.trim() || undefined,
          searchField,
          targetSortKey || undefined,
          dir,
        )
        setData(result)
        setPage(result.page)
      } catch {
        onError('Failed to load data.')
      } finally {
        if (opts?.silent) setRefreshing(false)
        else setLoading(false)
      }
    },
    [load, onError],
  )

  // Initial load only. Subsequent loads are triggered explicitly by user actions
  // (search, paging, page-size, sort, jump) so that a programmatic search-clear during a
  // jump can't reset the page back to 0. Guarded by a ref so it fires exactly once per mount:
  // `reload` is recreated whenever the parent passes a fresh inline `load` prop (e.g. on every
  // re-render), and without the guard that would re-trigger a skeleton load on each parent render.
  const didInitialLoad = useRef(false)
  const wasActive = useRef(false)
  const lastRefreshToken = useRef(refreshToken)
  useEffect(() => {
    if (forceLoading || !active) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) {
      if (didInitialLoad.current) {
        void reload(page, size, activeQuery, field, sortKey, direction, { silent: true })
      } else {
        didInitialLoad.current = true
        void reload(0, DEFAULT_PAGE_SIZE, '', 'all', sortKey, defaultSortDirection)
      }
    }
    wasActive.current = true
  }, [active, activeQuery, defaultSortDirection, direction, field, forceLoading, page, reload, size, sortKey])

  useEffect(() => {
    if (lastRefreshToken.current === refreshToken) return
    lastRefreshToken.current = refreshToken
    if (active && data) void reload(page, size, activeQuery, field, sortKey, direction, { silent: true })
  }, [active, activeQuery, data, direction, field, page, refreshToken, reload, size, sortKey])

  useEffect(
    () => () => {
      if (highlightTimer.current) clearTimeout(highlightTimer.current)
      if (searchTimer.current) clearTimeout(searchTimer.current)
    },
    [],
  )

  // Debounce the search box; a new search always starts from the first page.
  // Uses a silent reload so the existing rows stay visible with just a small spinner instead of
  // swapping the table out for skeletons on every keystroke.
  const onSearchChange = (value: string) => {
    setQuery(value)
    if (searchTimer.current) clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => {
      setActiveQuery(value)
      void reload(0, size, value, field, sortKey, direction, { silent: true })
    }, 300)
  }

  const onFieldChange = (next: string) => {
    setField(next)
    if (activeQuery.trim()) {
      void reload(0, size, activeQuery, next, sortKey, direction, { silent: true })
    }
  }

  const onSizeChange = (next: number) => {
    setSize(next)
    void reload(0, next, activeQuery, field, sortKey, direction)
  }

  const onToggleDirection = () => {
    const next = direction === 'desc' ? 'asc' : 'desc'
    setDirection(next)
    void reload(0, size, activeQuery, field, sortKey, next, { silent: true })
  }

  const onSort = (column: Column<T>) => {
    if (!column.sortKey) return
    const nextDirection = sortKey === column.sortKey
      ? direction === 'asc' ? 'desc' : 'asc'
      : column.initialSortDirection ?? 'asc'
    setSortKey(column.sortKey)
    setDirection(nextDirection)
    void reload(0, size, activeQuery, field, column.sortKey, nextDirection, { silent: true })
  }

  const searching = activeQuery.trim().length > 0
  const showLoading = forceLoading || loading
  const rows = data?.content ?? []
  const visibleColumns = columns.filter((column) => visibleColumnKeys.has(column.key))
  const hasActions = Boolean(rowActions) || canEdit
  const columnCount = visibleColumns.length + (expand ? 1 : 0) + (locate ? 1 : 0) + (hasActions ? 1 : 0)
  const firstRow = rows[0]
  const roomName = roomAccessor && firstRow ? roomAccessor(firstRow) : null
  const searchableColumns = columns.filter((column) => column.searchKey)
  const hasSortableColumns = columns.some((column) => column.sortKey)

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const toggleColumn = (key: string) => {
    setVisibleColumnKeys((previous) => {
      const next = new Set(previous)
      if (next.has(key)) {
        if (next.size > 1) next.delete(key)
      } else {
        next.add(key)
      }
      return next
    })
  }

  const remove = async (row: T) => {
    if (!onDelete) return
    if (!confirm('Delete this row? This action cannot be undone.')) return
    try {
      await onDelete(row)
      await reload(page, size, activeQuery, field, sortKey, direction)
    } catch {
      onError('Failed to delete row.')
    }
  }

  const jumpTo = async (row: T) => {
    if (!locate) return
    const id = rowKey(row)
    setJumpingId(id)
    try {
      const targetPage = await locate(row, size, direction)
      if (searchTimer.current) clearTimeout(searchTimer.current)
      setQuery('')
      setActiveQuery('')
      await reload(targetPage, size, '', field, sortKey, direction)
      setHighlightId(id)
      if (highlightTimer.current) clearTimeout(highlightTimer.current)
      highlightTimer.current = setTimeout(() => setHighlightId(null), 3000)
    } catch {
      onError('Failed to locate the selected row.')
    } finally {
      setJumpingId(null)
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          {roomAccessor &&
            (roomName ? (
              <Badge variant="outline" className="h-6 min-w-32 font-normal">
                Room: {roomName}
              </Badge>
            ) : (
              <Badge variant="outline" className="h-6 min-w-32 font-normal">
                <Skeleton className="h-3 w-20" />
              </Badge>
            ))}
          {searchableColumns.length > 0 && (
            <Select
              items={{
                all: 'All columns',
                ...Object.fromEntries(searchableColumns.map((c) => [c.searchKey!, c.label])),
              }}
              value={field}
              onValueChange={(v) => v && onFieldChange(v)}
            >
              <SelectTrigger size="sm" className="h-7 w-[150px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All columns</SelectItem>
                {searchableColumns.map((c) => (
                  <SelectItem key={c.searchKey} value={c.searchKey!}>
                    {c.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
          <div className="relative w-full sm:w-56">
            <Input
              className="h-7 w-full pr-7"
              placeholder="Search all rows…"
              value={query}
              onChange={(e) => onSearchChange(e.target.value)}
            />
            {refreshing && (
              <Loader2
                className="pointer-events-none absolute top-1/2 right-2 size-3.5 -translate-y-1/2 animate-spin text-muted-foreground"
                aria-label="Loading"
              />
            )}
          </div>
          {searching && data && (
            <span className="text-xs text-muted-foreground">
              {data.totalElements} match{data.totalElements === 1 ? '' : 'es'} across all rows
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
          {columnSelection && (
            <Popover>
              <PopoverTrigger
                render={
                  <Button variant="ghost" size="sm">
                    <Columns3 />
                    Columns
                  </Button>
                }
              />
              <PopoverContent side="bottom" align="end" className="w-56 p-2">
                <div className="grid gap-1">
                  {columns.map((column) => {
                    const checked = visibleColumnKeys.has(column.key)
                    return (
                      <label
                        key={column.key}
                        className="flex cursor-pointer items-center justify-between gap-3 rounded-md px-2 py-1.5 text-sm hover:bg-muted"
                      >
                        <span>{column.label}</span>
                        <Switch
                          checked={checked}
                          disabled={checked && visibleColumnKeys.size === 1}
                          onCheckedChange={() => toggleColumn(column.key)}
                        />
                      </label>
                    )
                  })}
                  {hasCustomColumnWidths && (
                    <Button variant="ghost" size="sm" className="mt-1 justify-start" onClick={resetColumnWidths}>
                      Reset column widths
                    </Button>
                  )}
                </div>
              </PopoverContent>
            </Popover>
          )}
          {!hasSortableColumns && (
            <Button
              variant="ghost"
              size="sm"
              disabled={showLoading}
              onClick={onToggleDirection}
              title={`Sort by ${sortLabel}: ${direction === 'desc' ? sortDescendingLabel.toLowerCase() : sortAscendingLabel.toLowerCase()} first`}
            >
              {direction === 'desc' ? <ArrowDown /> : <ArrowUp />}
              {direction === 'desc' ? sortDescendingLabel : sortAscendingLabel}
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            disabled={showLoading || refreshing}
            onClick={() => reload(page, size, activeQuery, field, sortKey, direction, { silent: true })}
          >
            <RotateCw className={showLoading || refreshing ? 'animate-spin' : ''} />
            Refresh
          </Button>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border">
        <Table
          ref={tableRef}
          className="min-w-[720px] table-fixed [&_td]:py-1.5 [&_th]:h-9 [&_tr]:border-border/50"
        >
          <TableHeader>
            <TableRow className="bg-muted/40">
              {expand && <TableHead className="w-[40px]" />}
              {visibleColumns.map((c, index) => {
                const nextColumn = visibleColumns[index + 1]
                return (
                <TableHead
                  key={c.key}
                  data-column-key={c.key}
                  className={cn('relative', c.headClassName)}
                  style={{ width: columnWidths[c.key] ?? c.defaultWidth }}
                  aria-sort={sortKey === c.sortKey ? (direction === 'asc' ? 'ascending' : 'descending') : undefined}
                >
                  {c.sortKey ? (
                    <button
                      type="button"
                      className="flex h-full w-full items-center gap-1 text-left font-medium hover:text-foreground"
                      disabled={showLoading}
                      onClick={() => onSort(c)}
                    >
                      <span className="truncate">{c.label}</span>
                      {sortKey === c.sortKey
                        ? direction === 'asc' ? <ArrowUp className="size-3.5" /> : <ArrowDown className="size-3.5" />
                        : <ArrowUpDown className="size-3.5 text-muted-foreground/50" />}
                    </button>
                  ) : <span className="block truncate">{c.label}</span>}
                  {nextColumn && (
                    <div
                      role="separator"
                      aria-label={`Resize ${c.label} column`}
                      aria-orientation="vertical"
                      aria-valuemin={MIN_COLUMN_WIDTH}
                      aria-valuemax={MAX_COLUMN_WIDTH}
                      aria-valuenow={columnWidths[c.key]}
                      tabIndex={0}
                      title="Drag to resize. Double-click to reset."
                      className={cn(
                        'absolute inset-y-0 -right-1 z-10 w-2 cursor-col-resize touch-none outline-none',
                        'before:absolute before:inset-y-2 before:left-1/2 before:w-px before:bg-border/70',
                        'hover:before:w-0.5 hover:before:bg-primary focus-visible:before:w-0.5 focus-visible:before:bg-primary',
                        resizingColumn === c.key && 'before:w-0.5 before:bg-primary',
                      )}
                      onPointerDown={(event) => startColumnResize(event, c.key, nextColumn.key)}
                      onPointerMove={resizeColumn}
                      onPointerUp={finishColumnResize}
                      onPointerCancel={finishColumnResize}
                      onLostPointerCapture={finishColumnResize}
                      onDoubleClick={(event) => {
                        event.preventDefault()
                        event.stopPropagation()
                        resetColumnBoundary(c.key, nextColumn.key)
                      }}
                      onKeyDown={(event) => resizeColumnWithKeyboard(event, c.key, nextColumn.key)}
                    />
                  )}
                </TableHead>
                )
              })}
              {locate && <TableHead className="w-[48px]" />}
              {hasActions && (
                <TableHead className="sticky right-0 z-20 w-[88px] border-l bg-[color-mix(in_oklab,var(--muted)_40%,var(--background))] pr-3 text-right shadow-[-4px_0_8px_-8px_rgba(0,0,0,0.5)]">
                  Actions
                </TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {showLoading &&
              Array.from({ length: SKELETON_ROWS }).map((_, i) => (
                <TableRow key={`skeleton-${i}`} className="h-[33px] hover:bg-transparent">
                  {expand && <TableCell className="w-[40px]" />}
                  {visibleColumns.map((c, idx) => (
                    <TableCell key={c.key} className={c.cellClassName}>
                      <Skeleton
                        className={cn(
                          'h-4',
                          c.skeleton ??
                            SKELETON_BAR_WIDTHS[(i * 7 + idx * 13) % SKELETON_BAR_WIDTHS.length],
                        )}
                      />
                    </TableCell>
                  ))}
                  {locate && <TableCell />}
                  {hasActions && (
                    <TableCell className="sticky right-0 z-10 border-l bg-background pr-3 text-right">
                      {canEdit && (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          className="text-muted-foreground"
                          aria-label="Delete row"
                          disabled
                        >
                          <Trash2 />
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}

            {!showLoading &&
              rows.map((row) => {
                const id = rowKey(row)
                const isExpanded = expandedIds.has(id)
                return (
                  <Fragment key={id}>
                    <TableRow className={cn('group h-[33px]', highlightId === id && 'bg-primary/10')}>
                      {expand && (
                        <TableCell className="w-[40px]">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="text-muted-foreground hover:text-foreground"
                            aria-label={isExpanded ? 'Collapse row' : 'Expand row'}
                            aria-expanded={isExpanded}
                            onClick={() => toggleExpanded(id)}
                          >
                            {isExpanded ? <ChevronDown /> : <ChevronRight />}
                          </Button>
                        </TableCell>
                      )}
                      {visibleColumns.map((c) => (
                        <TableCell key={c.key} className={c.cellClassName}>
                          <OverflowPreviewTrigger
                            handle={overflowPreviewHandle}
                            fallbackText={c.value(row)}
                          >
                            {c.render ? c.render(row) : c.value(row)}
                          </OverflowPreviewTrigger>
                        </TableCell>
                      ))}
                      {locate && (
                        <TableCell className="text-right">
                          {searching && (
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              className="text-muted-foreground hover:text-foreground"
                              aria-label="Jump to this row"
                              title="Jump to this row in the full list"
                              disabled={jumpingId === id}
                              onClick={() => jumpTo(row)}
                            >
                              <ArrowRightToLine />
                            </Button>
                          )}
                        </TableCell>
                      )}
                      {hasActions && (
                        <TableCell
                          className={cn(
                            'sticky right-0 z-10 border-l pr-3 text-right transition-colors',
                            highlightId === id
                              ? 'bg-primary/10'
                              : 'bg-background group-hover:bg-[color-mix(in_oklab,var(--muted)_50%,var(--background))] group-has-aria-expanded:bg-[color-mix(in_oklab,var(--muted)_50%,var(--background))]',
                          )}
                        >
                          <div className="flex justify-end gap-0.5">
                            {rowActions?.(row)}
                            {canEdit && (
                              <Button
                                variant="ghost"
                                size="icon-sm"
                                className="text-muted-foreground hover:text-destructive"
                                aria-label="Delete row"
                                onClick={() => remove(row)}
                              >
                                <Trash2 />
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      )}
                    </TableRow>
                    {expand && isExpanded && (
                      <TableRow className="hover:bg-transparent">
                        <TableCell colSpan={columnCount} className="bg-muted/20 p-0">
                          <div className="px-4 py-3">{expand(row)}</div>
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                )
              })}

            {!showLoading && rows.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={columnCount}
                  className="h-20 text-center text-muted-foreground"
                >
                  {searching ? 'No rows match your search.' : emptyLabel}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-2">
          <span>{data ? `${data.totalElements} total` : ''}</span>
          <span className="text-border">·</span>
          <span>Rows per page</span>
          <Select
            items={Object.fromEntries(PAGE_SIZES.map((s) => [String(s), String(s)]))}
            value={String(size)}
            onValueChange={(v) => v && onSizeChange(Number(v))}
          >
            <SelectTrigger size="sm" className="h-7 w-[72px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {PAGE_SIZES.map((s) => (
                <SelectItem key={s} value={String(s)}>
                  {s}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={showLoading || page <= 0}
            onClick={() => reload(page - 1, size, activeQuery, field, sortKey, direction)}
          >
            Previous
          </Button>
          <span>Page {page + 1} of {Math.max(data?.totalPages ?? 1, 1)}</span>
          <Button
            variant="outline"
            size="sm"
            disabled={showLoading || !data || page + 1 >= data.totalPages}
            onClick={() => reload(page + 1, size, activeQuery, field, sortKey, direction)}
          >
            Next
          </Button>
        </div>
      </div>

      <PreviewCard.Root handle={overflowPreviewHandle}>
        {({ payload }) => {
          if (!payload?.isOverflowing()) return null
          const text = payload.getText()
          return (
            <PreviewCard.Portal>
              <PreviewCard.Positioner
                side="top"
                sideOffset={6}
                align="start"
                collisionPadding={8}
                className="isolate z-50"
              >
                <PreviewCard.Popup className="max-h-64 w-max min-w-40 max-w-[min(32rem,calc(100vw-1rem))] cursor-text select-text overflow-auto whitespace-pre-wrap break-words rounded-md border bg-popover p-2 text-sm leading-5 text-popover-foreground shadow-md outline-none data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95">
                  {text}
                </PreviewCard.Popup>
              </PreviewCard.Positioner>
            </PreviewCard.Portal>
          )
        }}
      </PreviewCard.Root>
    </div>
  )
}

function OverflowPreviewTrigger({
  handle,
  fallbackText,
  children,
}: {
  handle: PreviewCard.Handle<OverflowPreviewPayload>
  fallbackText: string
  children: React.ReactNode
}) {
  const triggerRef = useRef<HTMLDivElement>(null)
  const payload: OverflowPreviewPayload = {
    getText: () => triggerRef.current?.innerText.trim() || fallbackText,
    isOverflowing: () => {
      const trigger = triggerRef.current
      if (!trigger) return false
      const cell = trigger.closest<HTMLElement>('[data-slot="table-cell"]')
      if (hasOverflow(trigger) || (cell && hasOverflow(cell))) return true
      return [...trigger.querySelectorAll<HTMLElement>('*')].some(hasOverflow)
    },
  }

  return (
    <PreviewCard.Trigger
      handle={handle}
      payload={payload}
      delay={350}
      closeDelay={250}
      render={
        <div
          ref={triggerRef}
          className="block min-w-0 max-w-full truncate outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
        />
      }
    >
      {children}
    </PreviewCard.Trigger>
  )
}
