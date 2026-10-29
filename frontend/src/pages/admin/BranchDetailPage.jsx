import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CircleOff, MapPin, Phone, Receipt, TriangleAlert, UserRound, Users, Wallet } from 'lucide-react'
import { Badge, Button, Card, EmptyState, Spinner, StatCard } from '../../components/ui'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney, STOCK_STATUS } from '../../utils/format'
import { getBranchDetail } from '../../services/branchService'

// Biểu đồ cột doanh thu theo ngày, vẽ bằng thẻ div (không cần thư viện biểu đồ).
function RevenueBars({ series }) {
  const max = Math.max(...series.map((d) => d.revenue), 1)
  return (
    <div className="flex h-44 items-end gap-1.5">
      {series.map((d) => (
        <div key={d.date} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5" title={`${d.dateLabel}: ${formatMoney(d.revenue)}`}>
          <div className="w-full rounded-t bg-accent/80 transition hover:bg-accent" style={{ height: `${Math.max((d.revenue / max) * 100, d.revenue ? 4 : 1)}%` }} />
          <span className="text-[10px] text-ink/50">{d.dateLabel}</span>
        </div>
      ))}
    </div>
  )
}

export default function BranchDetailPage() {
  const { branchId } = useParams()
  const navigate = useNavigate()
  const { data: b, loading, error, reload } = useAsyncData(() => getBranchDetail(branchId), [branchId])

  const back = (
    <Link to="/admin/branches" className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-ink/60 hover:text-accent">
      <ArrowLeft size={16} /> Tất cả chi nhánh
    </Link>
  )

  if (error) {
    return (
      <div>
        {back}
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được chi nhánh"
            description={error}
            action={
              <div className="flex gap-2">
                <Button variant="secondary" onClick={reload}>
                  Thử lại
                </Button>
                <Button onClick={() => navigate('/admin/branches')}>Về danh sách</Button>
              </div>
            }
          />
        </div>
      </div>
    )
  }

  if (loading) {
    return (
      <div>
        {back}
        <Spinner />
      </div>
    )
  }

  const warning = b.lowStock + b.outOfStock

  return (
    <div className="space-y-6">
      <div>
        {back}
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-display text-2xl font-bold text-ink">{b.name}</h1>
          <Badge tone="neutral">{b.branchId}</Badge>
          <Badge tone="ok">Đang hoạt động</Badge>
        </div>
        <ul className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm text-ink/70">
          <li className="flex items-center gap-1.5">
            <MapPin size={15} className="text-ink/40" /> {b.address}
          </li>
          <li className="flex items-center gap-1.5">
            <Phone size={15} className="text-ink/40" /> {b.phone}
          </li>
          <li className="flex items-center gap-1.5">
            <UserRound size={15} className="text-ink/40" /> Quản lý: {b.manager}
          </li>
          
        </ul>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Wallet} label="Doanh thu 30 ngày" value={<span className="whitespace-nowrap text-lg">{formatMoney(b.revenue)}</span>} hint={`${b.revenueChangePercent >= 0 ? '+' : ''}${b.revenueChangePercent}% so với kỳ trước`} />
        <StatCard icon={Receipt} tone="ok" label="Hóa đơn" value={b.orders} hint="30 ngày gần nhất" />
        <StatCard icon={Users} label="Nhân viên đang làm" value={b.employeeCount} />
        <StatCard icon={TriangleAlert} tone={b.outOfStock ? 'bad' : warning ? 'warn' : 'ok'} label="Hàng cần chú ý" value={warning} hint={`${b.outOfStock} hết hàng, ${b.lowStock} sắp hết`} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <Card title="Doanh thu 14 ngày gần nhất">
          <RevenueBars series={b.revenueSeries} />
        </Card>

        <Card title="Hàng cần nhập thêm" action={<Link to="/admin/inventory" className="text-xs font-semibold text-accent">Xem tồn kho</Link>}>
          {b.lowStockItems.length === 0 ? (
            <p className="py-6 text-center text-sm text-ink/60">Không có mã nào sắp hết.</p>
          ) : (
            <ul className="divide-y divide-ink/10">
              {b.lowStockItems.map((item) => (
                <li key={item.key} className="flex items-center justify-between gap-3 py-2.5 text-sm first:pt-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-ink">{item.productName}</p>
                    <p className="text-xs text-ink/50">
                      {item.productId} · tối thiểu {item.minQuantity}
                    </p>
                  </div>
                  <Badge tone={item.stockStatus === STOCK_STATUS.OUT ? 'bad' : 'warn'}>
                    {item.stockStatus === STOCK_STATUS.OUT ? 'Hết hàng' : `Còn ${item.quantity}`}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title={`Nhân viên (${b.employees.length})`} action={<Link to="/admin/employees" className="text-xs font-semibold text-accent">Quản lý nhân viên</Link>}>
          <ul className="divide-y divide-ink/10">
            {b.employees.map((e) => (
              <li key={e.employeeId} className="flex items-center justify-between gap-3 py-2.5 text-sm first:pt-0 last:pb-0">
                <div>
                  <p className="font-semibold text-ink">{e.employeeName}</p>
                  <p className="text-xs text-ink/50">{e.employeeId} · {e.phone}</p>
                </div>
                <Badge tone={e.position === 'Quản lý chi nhánh' ? 'accent' : 'neutral'}>{e.position}</Badge>
              </li>
            ))}
          </ul>
        </Card>

        <Card title="Bán tốt nhất 30 ngày" action={<Link to="/admin/employee-sales" className="text-xs font-semibold text-accent">Xem doanh số</Link>}>
          {b.topSellers.length === 0 ? (
            <p className="py-6 text-center text-sm text-ink/60">Chưa có hóa đơn.</p>
          ) : (
            <ol className="space-y-3">
              {b.topSellers.map((s) => (
                <li key={s.employeeId} className="flex items-center gap-3 text-sm">
                  <span className="grid size-8 place-items-center rounded-full bg-accent/15 font-bold text-accent">{s.rank}</span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-ink">{s.employeeName}</p>
                    <p className="text-xs text-ink/50">{s.orders} hóa đơn</p>
                  </div>
                  <span className="font-semibold tabular-nums text-ink">{formatMoney(s.revenue)}</span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </div>
  )
}