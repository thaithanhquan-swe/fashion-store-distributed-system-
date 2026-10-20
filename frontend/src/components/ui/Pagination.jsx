import { ChevronLeft, ChevronRight } from 'lucide-react'
import Button from './Button'

// <Pagination page={page} pageSize={8} total={rows.length} onChange={setPage} />
// page bắt đầu từ 1.
export default function Pagination({ page, pageSize, total, onChange }) {
  const pages = Math.max(1, Math.ceil(total / pageSize))
  const from = total === 0 ? 0 : (page - 1) * pageSize + 1
  const to = Math.min(page * pageSize, total)

  return (
    <div className="flex items-center justify-between gap-3 px-4 py-3 text-xs text-ink/60">
      <span>
        {from}-{to} / {total} mục
      </span>
      <div className="flex items-center gap-2">
        <span className="tabular-nums">
          Trang {page} / {pages}
        </span>
        <Button variant="secondary" size="sm" icon={ChevronLeft} aria-label="Trang trước" disabled={page <= 1} onClick={() => onChange(page - 1)} />
        <Button variant="secondary" size="sm" icon={ChevronRight} aria-label="Trang sau" disabled={page >= pages} onClick={() => onChange(page + 1)} />
      </div>
    </div>
  )
}