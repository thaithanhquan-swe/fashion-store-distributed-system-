import { useState } from 'react'
import EmptyState from './EmptyState'
import Pagination from './Pagination'
import Spinner from './Spinner'

// Bảng có phân trang sẵn.
// columns: [{ key, header, align?: 'right'|'center', width?, render?: (row) => JSX }]
// <DataTable columns={cols} rows={list} loading={loading} onRowClick={(r) => nav(r.id)} />
export default function DataTable({
  columns,
  rows,
  pageSize = 8,
  rowKey = 'id',
  loading = false,
  emptyText = 'Không có dữ liệu',
  onRowClick,
}) {
  const [page, setPage] = useState(1)
  const pages = Math.max(1, Math.ceil(rows.length / pageSize))
  const current = Math.min(page, pages) // lọc ra ít dòng hơn thì tự lùi về trang cuối
  const slice = rows.slice((current - 1) * pageSize, current * pageSize)

  const align = { right: 'text-right', center: 'text-center' }

  return (
    <div className="overflow-hidden rounded-xl border border-ink/10 bg-card">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="bg-ink/5">
              {columns.map((c) => (
                <th
                  key={c.key}
                  style={{ width: c.width }}
                  className={`whitespace-nowrap px-4 py-2.5 text-left text-[11px] font-bold uppercase tracking-wide text-ink/60 ${align[c.align] || ''}`}
                >
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {slice.map((row) => (
              <tr
                key={row[rowKey]}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={`border-t border-ink/10 ${onRowClick ? 'cursor-pointer hover:bg-ink/5' : ''}`}
              >
                {columns.map((c) => (
                  <td key={c.key} className={`px-4 py-3 align-middle ${align[c.align] || ''}`}>
                    {c.render ? c.render(row) : row[c.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {loading && <Spinner />}
      {!loading && rows.length === 0 && <EmptyState title={emptyText} />}
      <div className="border-t border-ink/10">
        <Pagination page={current} pageSize={pageSize} total={rows.length} onChange={setPage} />
      </div>
    </div>
  )
}