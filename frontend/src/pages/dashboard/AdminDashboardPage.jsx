import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  Boxes,
  CalendarDays,
  PackageX,
  Receipt,
  TrendingDown,
  Trophy,
  Users,
  Wallet,
} from 'lucide-react'
import { Badge, Card, Spinner, StatCard } from '../../components/ui'
import { formatMoney, STOCK_STATUS, STOCK_STATUS_LABEL } from '../../utils/format'
import * as dashboardService from '../../services/dashboardService'

// Tổng quan Admin: doanh thu, tồn kho và xếp hạng trên toàn chuỗi.

const Change = ({ value }) => {
  if (value === 0) return <span className="text-ink/50">không đổi so với kỳ trước</span>
  const up = value > 0
  return (
    <span className={up ? 'font-semibold text-ok' : 'font-semibold text-bad'}>
      {up ? '+' : ''}
      {value}% so với kỳ trước
    </span>
  )
}

const HorizontalBar = ({ percent, tone = 'bg-accent' }) => (
  <div className="h-2 w-full overflow-hidden rounded-full bg-ink/10">
    <div className={`h-2 rounded-full ${tone}`} style={{ width: `${Math.max(2, percent)}%` }} />
  </div>
)

export default function AdminDashboardPage() {
  const [result, setResult] = useState({ key: '', data: null, error: '' })
  const requestKey = 'admin-dashboard-30'
  const loading = result.key !== requestKey
  const data = result.data
  const error = result.error

  useEffect(() => {
    let alive = true
    const key = 'admin-dashboard-30'
    dashboardService
      .getAdminDashboard({ days: 30 })
      .then((payload) => {
        if (alive) setResult({ key, data: payload, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, data: null, error: err.message })
      })
    return () => {
      alive = false
    }
  }, [])

  const maxDaily = useMemo(
    () => Math.max(1, ...(data?.revenueSeries ?? []).map((day) => day.revenue)),
    [data],
  )
  const maxBranch = useMemo(
    () => Math.max(1, ...(data?.branchRevenue ?? []).map((branch) => branch.revenue)),
    [data],
  )
  const maxProduct = useMemo(
    () => Math.max(1, ...(data?.topProducts ?? []).map((product) => product.revenue)),
    [data],
  )
  const maxEmployee = useMemo(
    () => Math.max(1, ...(data?.employeeSales ?? []).map((employee) => employee.revenue)),
    [data],
  )

  if (loading) {
    return (
      <div>
        <h1 className="font-display text-2xl font-bold text-ink">Tổng quan</h1>
        <Spinner label="Đang tải số liệu toàn chuỗi..." />
      </div>
    )
  }

  if (error) {
    return (
      <div>
        <h1 className="font-display text-2xl font-bold text-ink">Tổng quan</h1>
        <div
          role="alert"
          className="mt-4 flex items-center gap-2 rounded-lg bg-bad/10 px-3 py-2.5 text-sm font-medium text-bad"
        >
          <AlertTriangle size={16} className="shrink-0" /> {error}
        </div>
      </div>
    )
  }

  const { summary, inventory, revenueSeries, branchRevenue, topProducts, lowStockItems, employeeSales } = data

  return (
    <div>
      <h1 className="font-display text-2xl font-bold text-ink">Tổng quan</h1>
      <p className="mt-1 text-sm text-ink/60">Toàn bộ chuỗi cửa hàng, 30 ngày gần nhất.</p>

      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={Wallet}
          label="Doanh thu"
          value={formatMoney(summary.revenue)}
          hint={<Change value={summary.revenueChangePercent} />}
          tone="accent"
        />
        <StatCard
          icon={Receipt}
          label="Số hóa đơn"
          value={summary.orders}
          hint={<Change value={summary.ordersChangePercent} />}
          tone="ok"
        />
        <StatCard
          icon={Users}
          label="Khách hàng"
          value={summary.customers}
          hint={<Change value={summary.customersChangePercent} />}
        />
        <StatCard
          icon={Boxes}
          label="Mã đang theo dõi"
          value={inventory.totalSkus}
          hint={`${inventory.lowStock} sắp hết · ${inventory.outOfStock} hết hàng`}
          tone={inventory.lowStock + inventory.outOfStock > 0 ? 'warn' : 'ok'}
        />
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-3">
        <Card title="Doanh thu theo ngày" className="xl:col-span-2">
          <div className="flex h-44 items-end gap-1">
            {revenueSeries.map((day) => (
              <div key={day.date} className="flex h-full flex-1 flex-col">
                <div className="flex flex-1 items-end">
                  <div
                    className="w-full rounded-t bg-accent/80"
                    style={{ height: `${Math.round((day.revenue / maxDaily) * 100)}%` }}
                    title={`${day.dateLabel}: ${formatMoney(day.revenue)}`}
                  />
                </div>
                <span className="mt-1 truncate text-center text-[10px] text-ink/50">{day.dateLabel}</span>
              </div>
            ))}
          </div>
          <p className="mt-3 text-xs text-ink/50">
            Trung bình {formatMoney(summary.averageDailyRevenue)} mỗi ngày · giá trị đơn trung bình{' '}
            {formatMoney(summary.averageOrderValue)}
          </p>
        </Card>

        <Card title="Doanh thu theo chi nhánh">
          <ul className="space-y-4">
            {branchRevenue.map((branch) => (
              <li key={branch.branchId}>
                <div className="flex items-baseline justify-between gap-3">
                  <span className="text-sm font-semibold text-ink">{branch.branchName}</span>
                  <span className="tabular-nums text-sm font-bold text-ink">{formatMoney(branch.revenue)}</span>
                </div>
                <div className="mt-1.5">
                  <HorizontalBar percent={(branch.revenue / maxBranch) * 100} />
                </div>
                <p className="mt-1 text-xs text-ink/50">
                  {branch.orders} hóa đơn · chiếm {branch.share}%
                </p>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-3">
        <Card title="Sản phẩm bán chạy">
          <ul className="space-y-4">
            {topProducts.map((product, index) => (
              <li key={product.productId}>
                <div className="flex items-baseline justify-between gap-3">
                  <span className="truncate text-sm font-semibold text-ink">
                    <span className="mr-1.5 text-ink/40">#{index + 1}</span>
                    {product.productName}
                  </span>
                  <span className="tabular-nums text-sm font-bold text-ink">{formatMoney(product.revenue)}</span>
                </div>
                <div className="mt-1.5">
                  <HorizontalBar percent={(product.revenue / maxProduct) * 100} />
                </div>
                <p className="mt-1 text-xs text-ink/50">Bán {product.quantity} sản phẩm</p>
              </li>
            ))}
          </ul>
        </Card>

        <Card
          title="Cảnh báo tồn kho"
          action={
            <Link to="/admin/inventory" className="text-xs font-semibold text-accent hover:underline">
              Xem tồn kho
            </Link>
          }
        >
          <ul className="space-y-3">
            {lowStockItems.map((item) => (
              <li key={item.key} className="flex items-center justify-between gap-3">
                <span className="min-w-0">
                  <span className="block truncate text-sm font-semibold text-ink">{item.productName}</span>
                  <span className="block text-xs text-ink/50">
                    {item.branchName} · tồn {item.quantity}/{item.minQuantity}
                  </span>
                </span>
                <Badge tone={item.stockStatus === STOCK_STATUS.OUT ? 'bad' : 'warn'}>
                  {STOCK_STATUS_LABEL[item.stockStatus]}
                </Badge>
              </li>
            ))}
          </ul>
        </Card>

        <Card
          title="Doanh số nhân viên"
          action={
            <Link to="/admin/employee-sales" className="text-xs font-semibold text-accent hover:underline">
              Xem chi tiết
            </Link>
          }
        >
          <ul className="space-y-4">
            {employeeSales.map((employee, index) => (
              <li key={employee.employeeId}>
                <div className="flex items-baseline justify-between gap-3">
                  <span className="truncate text-sm font-semibold text-ink">
                    <span className="mr-1.5 text-ink/40">#{index + 1}</span>
                    {employee.employeeName}
                  </span>
                  <span className="tabular-nums text-sm font-bold text-ink">
                    {formatMoney(employee.revenue)}
                  </span>
                </div>
                <div className="mt-1.5">
                  <HorizontalBar percent={(employee.revenue / maxEmployee) * 100} tone="bg-ok" />
                </div>
                <p className="mt-1 text-xs text-ink/50">
                  {employee.branchName} · {employee.orders} hóa đơn
                </p>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <Card className="mt-4">
        <div className="flex flex-wrap items-center gap-6 text-sm text-ink/70">
          <span className="flex items-center gap-2">
            <CalendarDays size={16} /> 30 ngày gần nhất
          </span>
          <span className="flex items-center gap-2">
            <Trophy size={16} /> {topProducts.length} sản phẩm bán chạy nhất
          </span>
          <span className="flex items-center gap-2">
            <TrendingDown size={16} /> {lowStockItems.length} mã đang cần nhập
          </span>
          <span className="flex items-center gap-2">
            <PackageX size={16} /> {inventory.outOfStock} mã hết hàng toàn chuỗi
          </span>
        </div>
      </Card>
    </div>
  )
}
