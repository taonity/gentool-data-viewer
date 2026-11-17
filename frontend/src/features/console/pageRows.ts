import type { Column } from './DataTab'
import type { PageResponse } from './types'

const collator = new Intl.Collator('en', { numeric: true, sensitivity: 'base' })

/** Apply shared table queries to endpoints that return a complete authorized list. */
export function pageRows<T>(
  rows: T[], columns: Column<T>[], rowKey: (row: T) => string,
  page: number, size: number, q = '', field?: string, sort?: string,
  direction = 'asc', exact = false,
): PageResponse<T> {
  const query = q.trim().toLowerCase()
  const searchColumns = columns.filter(column => column.searchKey && (!field || field === 'all' || column.searchKey === field))
  const filtered = rows.filter(row => !query || searchColumns.some(column => {
    const value = column.value(row).toLowerCase()
    return exact ? value === query : value.includes(query)
  }))
  const sortColumn = columns.find(column => column.sortKey === sort && column.sortable !== false)
  if (sortColumn) filtered.sort((a, b) => {
    const comparison = collator.compare(sortColumn.value(a), sortColumn.value(b)) || collator.compare(rowKey(a), rowKey(b))
    return direction === 'desc' ? -comparison : comparison
  })
  const totalPages = Math.ceil(filtered.length / size)
  const currentPage = Math.max(0, Math.min(page, totalPages - 1))
  return {
    content: filtered.slice(currentPage * size, (currentPage + 1) * size),
    page: currentPage, size, totalElements: filtered.length, totalPages,
    hasMore: currentPage + 1 < totalPages,
  }
}
