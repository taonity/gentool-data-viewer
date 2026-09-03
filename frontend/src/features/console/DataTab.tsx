'use client'

import { Fragment, useCallback, useEffect, useRef, useState } from 'react'
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
  skeleton?: string
  /** Backend field id this column maps to; when set, the column is offered as a search scope. */
  searchKey?: string
  sortKey?: string
  initialSortDirection?: 'asc' | 'desc'
  defaultVisible?: boolean
}

type DataTabProps<T> = {
  columns: Column<T>[]
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
  rowKey,
  load,
  locate,
  expand,
  roomAccessor,
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
  const highlightTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

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
  const columnCount = visibleColumns.length + (expand ? 1 : 0) + (locate ? 1 : 0) + (canEdit ? 1 : 0)
  const firstRow = rows[0]
  const roomName = roomAccessor && firstRow ? roomAccessor(firstRow) : null
  const searchableColumns = columns.filter((c) => c.searchKey)
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
        <Table className="min-w-[720px] table-fixed [&_td]:py-1.5 [&_th]:h-9 [&_tr]:border-border/50">
          <TableHeader>
            <TableRow className="bg-muted/40">
              {expand && <TableHead className="w-[40px]" />}
              {visibleColumns.map((c) => (
                <TableHead
                  key={c.key}
                  className={c.headClassName}
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
                  ) : c.label}
                </TableHead>
              ))}
              {locate && <TableHead className="w-[48px]" />}
              {canEdit && <TableHead className="w-[64px] pr-4 text-right">Actions</TableHead>}
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
                  {canEdit && (
                    <TableCell className="pr-4 text-right">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-muted-foreground"
                        aria-label="Delete row"
                        disabled
                      >
                        <Trash2 />
                      </Button>
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
                    <TableRow className={cn('h-[33px]', highlightId === id && 'bg-primary/10')}>
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
                          {c.render ? c.render(row) : c.value(row)}
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
                      {canEdit && (
                        <TableCell className="pr-4 text-right">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="text-muted-foreground hover:text-destructive"
                            aria-label="Delete row"
                            onClick={() => remove(row)}
                          >
                            <Trash2 />
                          </Button>
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
    </div>
  )
}
