import { Link } from 'react-router-dom'
import { ArrowRight, CircleOff, MapPin, Phone, UserRound } from 'lucide-react'
import { Badge, Button, EmptyState, Spinner } from '../../components/ui'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney } from '../../utils/format'
import { getBranchList } from '../../services/branchService'

// Một ô số liệu nhỏ trong thẻ chi nhánh
function Metric({ label, value }) {
  return (
    <div>
      <p className="text-xs text-ink/50">{label}</p>
      <p className="whitespace-nowrap font-display text-base font-bold tabular-nums text-ink">{value}</p>
    </div>
  )
}

export default function BranchListPage() {
  const { data, loading, error, reload } = useAsyncData(getBranchList, [])

  return (
    <div>
      <header className="mb-6">
        <h1 className="font-display text-2xl font-bold text-ink">Chi nhánh</h1>
        <p className="mt-1 text-sm text-ink/60">Ba chi nhánh trong chuỗi, số liệu 30 ngày gần nhất.</p>
      </header>

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách chi nhánh"
            description={error}
            action={
              <Button variant="secondary" onClick={reload}>
                Thử lại
              </Button>
            }
          />
        </div>
      ) : loading ? (
        <Spinner />
      ) : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {data.map((b) => {
            const warning = b.lowStock + b.outOfStock
            return (
              <Link
                key={b.branchId}
                to={`/admin/branches/${b.branchId}`}
                className="group flex flex-col rounded-xl border border-ink/10 bg-card p-5 transition hover:border-accent hover:shadow-md"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h2 className="font-display text-xl font-bold text-ink">{b.name}</h2>
                    <p className="text-xs font-semibold text-ink/50">{b.branchId}</p>
                  </div>
                  <Badge tone="ok">Đang hoạt động</Badge>
                </div>

                <ul className="mt-4 space-y-2 text-sm text-ink/70">
                  <li className="flex gap-2">
                    <MapPin size={16} className="mt-0.5 shrink-0 text-ink/40" /> {b.address}
                  </li>
                  <li className="flex gap-2">
                    <Phone size={16} className="mt-0.5 shrink-0 text-ink/40" /> {b.phone}
                  </li>
                  <li className="flex gap-2">
                    <UserRound size={16} className="mt-0.5 shrink-0 text-ink/40" /> Quản lý: {b.manager}
                  </li>
                </ul>

                <div className="mt-5 grid grid-cols-2 gap-4 border-t border-ink/10 pt-4">
                  <Metric label="Doanh thu" value={formatMoney(b.revenue)} />
                  <Metric label="Hóa đơn" value={b.orders} />
                  <Metric label="Nhân viên" value={b.employeeCount} />
                  <div>
                    <p className="text-xs text-ink/50">Hàng cần chú ý</p>
                    <p className="mt-0.5">
                      {warning ? <Badge tone={b.outOfStock ? 'bad' : 'warn'}>{warning} mã</Badge> : <Badge tone="ok">Ổn định</Badge>}
                    </p>
                  </div>
                </div>

                <span className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-accent">
                  Xem chi tiết <ArrowRight size={16} className="transition group-hover:translate-x-1" />
                </span>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}