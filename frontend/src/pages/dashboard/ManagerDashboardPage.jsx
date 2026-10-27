import { Link } from 'react-router-dom'
import { AlertTriangle, CircleOff, Receipt, Users, Wallet } from 'lucide-react'
import { Badge, Button, Card, EmptyState, Spinner, StatCard } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney, STOCK_STATUS } from '../../utils/format'
import { getBranch } from '../../config/branches'
import { getManagerDashboard } from '../../services/dashboardService'

// Tổng quan Manager: doanh thu 30 ngày, tồn kho cảnh báo, sản phẩm bán chạy và nhân viên của chi nhánh mình.
const DAYS = 30

const Change = ({ value }) => {
  if (value === 0) return <span className="text-ink/50">không đổi so với kỳ trước</span>
  return (
    <span className={value > 0 ? 'font-semibold text-ok' : 'font-semibold text-bad'}>
      {value > 0 ? '+' : ''}
      {value}% so với kỳ trước
    </span>
  )
}

function RevenueBars({ series }) {
  const max = Math.max(...series.map((d) => d.revenue), 1)
  return (
    <div className="flex h-44 items-end gap-1">
      {series.map((d, i) => (
        <div key={d.date} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5" title={`${d.dateLabel}: ${formatMoney(d.revenue)}`}>
          <div className="w-full rounded-t bg-accent/80 transition hover:bg-accent" style={{ height: `${Math.max((d.revenue / max) * 100, d.revenue ? 4 : 1)}%` }} />
          <span className="h-3 whitespace-nowrap text-[10px] text-ink/50">{i % 5 === 0 ? d.dateLabel : ''}</span>
        </div>
      ))}
    </div>
  )
}

export default function ManagerDashboardPage() {
  const { user } = useAuth()
  const branchId = user.branchId
  const { data, loading, error, reload } = useAsyncData(() => getManagerDashboard({ days: DAYS }), [branchId])

  if (error) {
    return (
      <div className="rounded-xl border border-ink/10 bg-card">
        <EmptyState
          icon={CircleOff}
          title="Không tải được tổng quan"
          description={error}
          action={
            <Button variant="secondary" onClick={reload}>
              Thử lại
            </Button>
          }
        />
      </div>
    )
  }
  if (loading || !data) return <Spinner />

  const { summary, series, top, employees, inventory, low } = data

  return (
    <div>
      <header className="mb-6">
        <h1 className="font-display text-2xl font-bold text-ink">Tổng quan chi nhánh</h1>
        <p className="mt-1 text-sm text-ink/60">
          {getBranch(branchId)?.name ?? branchId} · {DAYS} ngày gần nhất
        </p>
      </header>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Wallet} label="Doanh thu" value={<span className="whitespace-nowrap text-xl">{formatMoney(summary.revenue)}</span>} hint={<Change value={summary.revenueChangePercent} />} />
        <StatCard icon={Receipt} label="Hóa đơn" value={summary.orders} hint={<Change value={summary.ordersChangePercent} />} />
        <StatCard icon={Users} label="Khách mua hàng" value={summary.customers} hint={<Change value={summary.customersChangePercent} />} />
        <StatCard
          icon={AlertTriangle}
          label="Cần nhập hàng"
          tone={inventory.outOfStock ? 'bad' : inventory.lowStock ? 'warn' : 'ok'}
          value={inventory.lowStock + inventory.outOfStock}
          hint={`${inventory.outOfStock} hết hàng · ${inventory.lowStock} sắp hết`}
        />
      </div>

      <div className="mb-6 grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card title="Doanh thu theo ngày">
            <RevenueBars series={series} />
          </Card>
        </div>
        <Card title="Tồn kho cần chú ý" action={<Link to="/manager/low-stock" className="text-xs font-semibold text-accent hover:underline">Xem tất cả</Link>}>
          {low.length === 0 ? (
            <p className="py-6 text-center text-sm text-ink/50">Tồn kho ổn định</p>
          ) : (
            <ul className="divide-y divide-ink/10">
              {low.slice(0, 5).map((r) => (
                <li key={r.key} className="flex items-center justify-between gap-2 py-2.5 text-sm">
                  <span className="min-w-0 truncate font-medium text-ink">{r.productName}</span>
                  <span className="flex shrink-0 items-center gap-2">
                    <span className="tabular-nums text-ink/60">{r.quantity}</span>
                    <Badge tone={r.stockStatus === STOCK_STATUS.OUT ? 'bad' : 'warn'}>{r.stockStatus === STOCK_STATUS.OUT ? 'Hết' : 'Sắp hết'}</Badge>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Sản phẩm bán chạy">
          <ol className="divide-y divide-ink/10">
            {top.map((p) => (
              <li key={p.productId} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                <span className="min-w-0 truncate">
                  <span className="mr-2 font-bold text-ink/40">{p.rank}</span>
                  <span className="font-medium text-ink">{p.productName}</span>
                </span>
                <span className="shrink-0 whitespace-nowrap text-right tabular-nums">
                  <span className="font-semibold">{formatMoney(p.revenue)}</span>
                  <span className="ml-2 text-xs text-ink/50">{p.quantity} cái</span>
                </span>
              </li>
            ))}
          </ol>
        </Card>
        <Card title="Doanh số nhân viên">
          <ol className="divide-y divide-ink/10">
            {employees.map((e) => (
              <li key={e.employeeId} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                <span className="min-w-0 truncate">
                  <span className="mr-2 font-bold text-ink/40">{e.rank}</span>
                  <span className="font-medium text-ink">{e.employeeName}</span>
                  <span className="ml-2 text-xs text-ink/50">{e.position}</span>
                </span>
                <span className="shrink-0 whitespace-nowrap text-right tabular-nums">
                  <span className="font-semibold">{formatMoney(e.revenue)}</span>
                  <span className="ml-2 text-xs text-ink/50">{e.orders} đơn</span>
                </span>
              </li>
            ))}
          </ol>
        </Card>
      </div>
    </div>
  )
}