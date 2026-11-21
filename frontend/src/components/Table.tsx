import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  cell: (row: T) => ReactNode
  /** If set, the header is a button that calls onSort(sortKey). */
  sortKey?: string
}

interface TableProps<T> {
  caption: string
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string
  /** Currently sorted column (ascending). */
  sortBy?: string
  onSort?: (sortKey: string) => void
}

/** Plain accessible table: caption for screen readers, aria-sort on the sorted column. */
export function Table<T>({ caption, columns, rows, rowKey, sortBy, onSort }: TableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className="px-4 py-3"
                aria-sort={col.sortKey && col.sortKey === sortBy ? 'ascending' : undefined}
              >
                {col.sortKey && onSort ? (
                  <button
                    type="button"
                    onClick={() => onSort(col.sortKey!)}
                    className="inline-flex items-center gap-1 uppercase tracking-wide hover:text-slate-900"
                  >
                    {col.header}
                    <span aria-hidden="true">{col.sortKey === sortBy ? '▲' : ''}</span>
                  </button>
                ) : (
                  col.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row) => (
            <tr key={rowKey(row)} className="hover:bg-slate-50">
              {columns.map((col) => (
                <td key={col.key} className="px-4 py-3 align-middle">
                  {col.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
