import { describe, expect, it } from 'vitest'
import type { Column } from './DataTab'
import { pageRows } from './pageRows'

type Row = { id: string; name: string; email: string; role: string }
const columns: Column<Row>[] = ['name', 'email', 'role'].map(key => ({
  key, label: key, searchKey: key, sortKey: key, defaultWidth: 160,
  value: row => row[key as 'name' | 'email' | 'role'],
}))
const rows: Row[] = Array.from({ length: 61 }, (_, index) => ({
  id: String(index), name: `User ${index + 1}`, email: `person${index + 1}@example.com`,
  role: index === 60 ? 'Editor' : 'Viewer',
}))
const key = (row: Row) => row.id

describe('complete-list table queries', () => {
  it('searches the entire collection before pagination and does not mutate source order', () => {
    const result = pageRows(rows, columns, key, 0, 20, 'editor', undefined, 'name')
    expect(result.content.map(key)).toEqual(['60'])
    expect(result.totalElements).toBe(1)
    expect(rows[0]!.name).toBe('User 1')
  })

  it('limits search to the chosen scope, with case-insensitive exact matching', () => {
    expect(pageRows(rows, columns, key, 0, 20, 'editor', 'email').content).toEqual([])
    expect(pageRows(rows, columns, key, 0, 20, ' USER 1 ', 'name', 'name', 'asc', true).content.map(key)).toEqual(['0'])
    expect(pageRows(rows, columns, key, 0, 20, 'User 1', 'name').totalElements).toBe(11)
  })

  it('sorts numerically before slicing pages in either direction', () => {
    const result = pageRows(rows, columns, key, 1, 20, '', undefined, 'name', 'asc')
    expect(result.content[0]!.name).toBe('User 21')
    expect(result.content.at(-1)?.name).toBe('User 40')
    expect(result).toMatchObject({ page: 1, size: 20, totalElements: 61, totalPages: 4, hasMore: true })
    expect(pageRows(rows, columns, key, 0, 20, '', undefined, 'name', 'desc').content[0]!.name).toBe('User 61')
  })

  it('clamps pages after filtering or removing the last row and handles empty lists', () => {
    expect(pageRows(rows.slice(0, 60), columns, key, 3, 20).page).toBe(2)
    expect(pageRows(rows, columns, key, 3, 20, 'editor').page).toBe(0)
    expect(pageRows([], columns, key, 3, 20)).toEqual({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasMore: false })
  })

  it('uses row IDs to keep equal sort values stable across refreshed lists', () => {
    const tied = rows.slice(0, 12).map(row => ({ ...row, name: 'Same' }))
    const query = (input: Row[]) => pageRows(input, columns, key, 0, 20, '', undefined, 'name').content.map(key)
    expect(query([...tied].reverse())).toEqual(query(tied))
    expect(query(tied).slice(0, 3)).toEqual(['0', '1', '2'])
  })
})
