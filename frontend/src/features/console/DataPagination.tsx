'use client'

import { useId, useState, type Ref } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const PAGE_SIZES = [20, 50, 100]

type DataPaginationProps = {
  page: number
  size: number
  rowCount: number
  totalElements?: number
  totalPages?: number
  loading: boolean
  position: 'top' | 'bottom'
  paginationRef?: Ref<HTMLElement>
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
}

export function DataPagination({
  page, size, rowCount, totalElements, totalPages, loading, position,
  paginationRef, onPageChange, onSizeChange,
}: DataPaginationProps) {
  const id = useId()
  const [draft, setDraft] = useState<string | null>(null)
  const pageCount = Math.max(totalPages ?? 1, 1)
  const firstRow = rowCount > 0 ? page * size + 1 : 0
  const lastRow = rowCount > 0 ? page * size + rowCount : 0

  return (
    <nav
      ref={paginationRef}
      aria-label={`Pagination ${position === 'top' ? 'above' : 'below'} records`}
      aria-busy={loading}
      tabIndex={-1}
      className="data-pagination grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 text-xs text-muted-foreground sm:flex sm:flex-wrap sm:justify-between"
    >
      <span className="min-w-0 tabular-nums" role={position === 'top' ? 'status' : undefined}>
        {totalElements === undefined ? 'Loading…' : `${firstRow.toLocaleString()}–${lastRow.toLocaleString()} of ${totalElements.toLocaleString()}`}
      </span>
      <div className="flex items-center gap-2 sm:ml-auto">
        <span className="hidden sm:inline">Rows per page</span>
        <Select
          items={Object.fromEntries(PAGE_SIZES.map((value) => [String(value), String(value)]))}
          value={String(size)}
          onValueChange={(value) => value && onSizeChange(Number(value))}
          disabled={loading}
        >
          <SelectTrigger size="sm" className="h-7 w-18" aria-label="Rows per page"><SelectValue /></SelectTrigger>
          <SelectContent>
            {PAGE_SIZES.map((value) => <SelectItem key={value} value={String(value)}>{value}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      <div className="flex items-center gap-1 sm:gap-2">
        <Button variant="outline" size="sm" className="px-1.5 sm:px-2.5" aria-label="Previous page" disabled={loading || page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft aria-hidden="true" /><span className="hidden sm:inline">Previous</span>
        </Button>
        <form
          className="flex shrink-0 items-center gap-1"
          onSubmit={(event) => {
            event.preventDefault()
            const requested = Number(draft ?? page + 1)
            setDraft(null)
            if (Number.isInteger(requested) && requested >= 1 && requested <= pageCount && requested !== page + 1) {
              onPageChange(requested - 1)
            }
          }}
        >
          <label htmlFor={id} className="hidden sm:inline">Page</label>
          <Input
            id={id}
            type="number"
            inputMode="numeric"
            aria-label="Page number"
            min={1}
            max={pageCount}
            value={draft ?? String(page + 1)}
            disabled={loading || totalPages === undefined || totalPages === 0}
            onChange={(event) => setDraft(event.target.value)}
            onBlur={() => setDraft(null)}
            className="h-7 w-14 shrink-0 px-1 text-center text-xs tabular-nums"
          />
          <span className="inline-flex shrink-0 items-baseline gap-1 whitespace-nowrap tabular-nums">
            <span aria-hidden="true">/</span>
            <span className="min-w-[4ch] text-right">{pageCount}</span>
          </span>
        </form>
        <Button variant="outline" size="sm" className="px-1.5 sm:px-2.5" aria-label="Next page" disabled={loading || totalPages === undefined || page + 1 >= pageCount} onClick={() => onPageChange(page + 1)}>
          <span className="hidden sm:inline">Next</span><ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </nav>
  )
}
