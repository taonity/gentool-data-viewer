'use client'

import { useId } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const PAGE_SIZES = [20, 50, 100]

export function DataPagination({ page, pageInput, size, totalElements, totalPages, loading, position, onPageInput, onPageSubmit, onPageChange, onSizeChange }: {
  page: number
  pageInput: string
  size: number
  totalElements?: number
  totalPages?: number
  loading: boolean
  position: 'top' | 'bottom'
  onPageInput: (value: string) => void
  onPageSubmit: (event: React.FormEvent<HTMLFormElement>) => void
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
}) {
  const id = useId()
  const pageCount = Math.max(totalPages ?? 1, 1)

  return (
    <nav aria-label={`Pagination ${position === 'top' ? 'above' : 'below'} records`} aria-busy={loading}
      className="data-pagination grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 text-xs text-muted-foreground sm:flex sm:flex-wrap sm:justify-between">
      <span className="min-w-0 tabular-nums" aria-live={position === 'top' ? 'polite' : 'off'}>
        {totalElements === undefined ? 'Loading…' : `${totalElements.toLocaleString()} total`}
      </span>
      <div className="flex items-center gap-2 sm:ml-auto">
        <span className="hidden sm:inline">Rows per page</span>
        <Select items={Object.fromEntries(PAGE_SIZES.map(value => [String(value), String(value)]))}
          value={String(size)} onValueChange={value => value && onSizeChange(Number(value))} disabled={loading}>
          <SelectTrigger size="sm" className="h-7 w-18" aria-label="Rows per page"><SelectValue /></SelectTrigger>
          <SelectContent>{PAGE_SIZES.map(value => <SelectItem key={value} value={String(value)}>{value}</SelectItem>)}</SelectContent>
        </Select>
      </div>
      <div className="flex items-center gap-1 sm:gap-2">
        <Button variant="outline" size="sm" className="px-1.5 sm:px-2.5" aria-label="Previous page" disabled={loading || page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft /><span className="hidden sm:inline">Previous</span>
        </Button>
        <form className="flex items-center gap-1 tabular-nums" onSubmit={onPageSubmit}>
          <label htmlFor={id} className="sr-only sm:not-sr-only">Page</label>
          <Input id={id} aria-label="Page number" type="number" inputMode="numeric" min={1} max={pageCount}
            value={pageInput} disabled={loading || totalPages === undefined}
            onChange={event => onPageInput(event.target.value)} onBlur={() => onPageInput(String(page + 1))}
            className="h-7 w-10 px-1 text-center text-xs sm:w-14" />
          <span className="whitespace-nowrap"><span className="hidden sm:inline">of </span><span className="sm:hidden">/ </span>{pageCount}</span>
        </form>
        <Button variant="outline" size="sm" className="px-1.5 sm:px-2.5" aria-label="Next page" disabled={loading || totalPages === undefined || page + 1 >= pageCount} onClick={() => onPageChange(page + 1)}>
          <span className="hidden sm:inline">Next</span><ChevronRight />
        </Button>
      </div>
    </nav>
  )
}
