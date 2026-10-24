import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, Receipt, ShoppingBag, Users, Wallet } from 'lucide-react'
import { Card, DataTable, Select, Spinner, StatCard } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { BRANCHES } from '../../config/branches'
import { formatDate, formatMoney } from '../../utils/format'
import * as statisticsService from '../../services/statisticsService'

// Thống kê bán hàng. Manager xem chi nhánh của mình, admin xem toàn chuỗi.

const PERIOD_OPTIONS = [
  { value: '7', label: '7 ngày gần nhất' },
  { value: '30', label: '30 ngày gần nhất' },
  { value: '90', label: '90 ngày gần nhất' },
]

// Khoá của lần tải dữ liệu, dùng để biết dữ liệu trên màn hình còn khớp bộ lọc hay không.
const buildKey = (branch, days) => `${branch ?? 'all'}|${days}`

export default function StatisticsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'admin'
  const [days, setDays] = useState('30')
  const [branchFilter, setBranchFilter] = useState('')
  const [result, setResult] = useState({ key: '', summary: null, series: [], topProducts: [], error: '' })

  const scopeBranch = isAdmin ? branchFilter || null : user.branchId ?? null
  const requestKey = buildKey(scopeBranch, days)
  const loading = result.key !== requestKey
  const summary = result.summary
  const series = result.series
  const topProducts = result.topProducts
  const error = result.error

  useEffect(() => {
    let alive = true
    const key = buildKey(scopeBranch, days)
    const period = Number(days)
    Promise.all([
      statisticsService.getRevenueSummary({ branchId: scopeBranch, days: period }),
      statisticsService.getRevenueSeries({ branchId: scopeBranch, days: period }),
      statisticsService.getTopProducts({ branchId: scopeBranch, days: period, limit: 10 }),
    ])
      .then(([sum, daily, products]) => {
        if (alive) setResult({ key, summary: sum, series: daily, topProducts: products, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, summary: null, series: [], topProducts: [], error: err.message })
      })
    return () => {
      alive = false
    }
  }, [scopeBranch, days])

  const maxDaily = useMemo(() => Math.max(1, ...series.map((day) => day.revenue)), [series])

  const branchOptions = useMemo(
    () => [
      { value: '', label: 'Tất cả chi nhánh' },
      ...BRANCHES.map((branch) => ({ value: branch.id, label: branch.name })),
    ],
    [],
  )

  const dailyColumns = [
    {
      key: 'dateLabel',
      header: 'Ngày',
      render: (row) => <span className="font-medium text-ink">{formatDate(row.date)}</span>,
    },
    {
      key: 'revenue',
      header: 'Doanh thu',
      align: 'right',
      render: (row) => <span className="font-semibold tabular-nums">{formatMoney(row.revenue)}</span>,
    },
    {
      key: 'orders',
      header: 'Số hóa đơn',
      align: 'right',
      render: (row) => <span className="tabular-nums text-ink/70">{row.orders}</span>,
    },
  ]

  const productColumns = [
    { key: 'rank', header: '#', width: '48px', render: (row) => row.rank },
    { key: 'productId', header: 'Mã SP', width: '90px' },
    { key: 'productName', header: 'Tên sản phẩm' },
    {
      key: 'quantity',
      header: 'Số lượng bán',
      align: 'right',
      render: (row) => <span className="tabular-nums">{row.quantity}</span>,
    },
    {
      key: 'invoices',
      header: 'Số hóa đơn',
      align: 'right',
      render: (row) => <span className="tabular-nums text-ink/70">{row.invoices}</span>,
    },
    {
      key: 'revenue',
      header: 'Doanh thu',
      align: 'right',
      render: (row) => <span className="font-semibold tabular-nums">{formatMoney(row.revenue)}</span>,
    },
  ]

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Thống kê</h1>
          <p className="mt-1 text-sm text-ink/60">
            {isAdmin
              ? 'Doanh thu và sản phẩm bán chạy trên toàn chuỗi.'
              : 'Doanh thu và sản phẩm bán chạy tại chi nhánh của bạn.'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {isAdmin && (
            <Select
              value={branchFilter}
              onChange={(event) => setBranchFilter(event.target.value)}
              options={branchOptions}
              className="w-56"
              aria-label="Lọc theo chi nhánh"
            />
          )}
          <Select
            value={days}
            onChange={(event) => setDays(event.target.value)}
            options={PERIOD_OPTIONS}
            className="w-48"
            aria-label="Chọn khoảng thời gian"
          />
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="mt-4 flex items-center gap-2 rounded-lg bg-bad/10 px-3 py-2.5 text-sm font-medium text-bad"
        >
          <AlertTriangle size={16} className="shrink-0" /> {error}
        </div>
      )}

      {loading ? (
        <Spinner label="Đang tải số liệu thống kê..." />
      ) : (
        <>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              icon={Wallet}
              label="Doanh thu"
              value={formatMoney(summary.revenue)}
              hint={`Kỳ trước ${formatMoney(summary.previousRevenue)}`}
              tone="accent"
            />
            <StatCard
              icon={Receipt}
              label="Số hóa đơn"
              value={summary.orders}
              hint={`Kỳ trước ${summary.previousOrders} hóa đơn`}
              tone="ok"
            />
            <StatCard
              icon={ShoppingBag}
              label="Giá trị đơn trung bình"
              value={formatMoney(summary.averageOrderValue)}
              hint={`Trung bình ${formatMoney(summary.averageDailyRevenue)} mỗi ngày`}
            />
            <StatCard
              icon={Users}
              label="Khách hàng"
              value={summary.customers}
              hint="Khách hàng có phát sinh mua hàng"
              tone="warn"
            />
          </div>

          <Card title="Doanh thu theo ngày" className="mt-4">
            <div className="flex h-44 items-end gap-1">
              {series.map((day) => (
                <div key={day.date} className="flex h-full flex-1 flex-col">
                  <div className="flex flex-1 items-end">
                    <div
                      className="w-full rounded-t bg-accent/80"
                      style={{ height: `${Math.round((day.revenue / maxDaily) * 100)}%` }}
                      title={`${day.dateLabel}: ${formatMoney(day.revenue)}`}
                    />
                  </div>
                  {series.length <= 31 && (
                    <span className="mt-1 truncate text-center text-[10px] text-ink/50">{day.dateLabel}</span>
                  )}
                </div>
              ))}
            </div>
          </Card>

          <div className="mt-4 grid gap-4 xl:grid-cols-2">
            <Card title="Bảng doanh thu theo ngày">
              <DataTable
                columns={dailyColumns}
                rows={series}
                rowKey="date"
                pageSize={8}
                emptyText="Khoảng thời gian này chưa có doanh thu"
              />
            </Card>

            <Card title="Sản phẩm bán chạy">
              <DataTable
                columns={productColumns}
                rows={topProducts}
                rowKey="productId"
                pageSize={8}
                emptyText="Khoảng thời gian này chưa có sản phẩm bán chạy"
              />
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
