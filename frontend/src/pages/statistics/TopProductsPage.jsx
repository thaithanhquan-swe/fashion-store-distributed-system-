import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, Package, ShoppingBag, Trophy, Wallet } from 'lucide-react'
import { Card, DataTable, Select, Spinner, StatCard } from '../../components/ui'
import { BRANCHES } from '../../config/branches'
import { formatMoney } from '../../utils/format'
import * as statisticsService from '../../services/statisticsService'

// Sản phẩm bán chạy trên toàn chuỗi, xếp theo doanh thu hoặc số lượng bán.

const PERIOD_OPTIONS = [
  { value: '7', label: '7 ngày gần nhất' },
  { value: '30', label: '30 ngày gần nhất' },
  { value: '90', label: '90 ngày gần nhất' },
]

const SORT_OPTIONS = [
  { value: 'revenue', label: 'Theo doanh thu' },
  { value: 'quantity', label: 'Theo số lượng bán' },
]

export default function TopProductsPage() {
  const [days, setDays] = useState('30')
  const [branchFilter, setBranchFilter] = useState('')
  const [sortBy, setSortBy] = useState('revenue')
  const [result, setResult] = useState({ key: '', rows: [], error: '' })

  const requestKey = `top-products|${days}|${branchFilter}|${sortBy}`
  const loading = result.key !== requestKey
  const rows = result.rows
  const error = result.error

  useEffect(() => {
    let alive = true
    const key = `top-products|${days}|${branchFilter}|${sortBy}`
    const period = Number(days)
    statisticsService
      .getTopProducts({ branchId: branchFilter || null, days: period, limit: 50, sortBy })
      .then((list) => {
        if (alive) setResult({ key, rows: list, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, rows: [], error: err.message })
      })
    return () => {
      alive = false
    }
  }, [days, branchFilter, sortBy])

  const totalQuantity = useMemo(() => rows.reduce((sum, row) => sum + row.quantity, 0), [rows])
  const totalRevenue = useMemo(() => rows.reduce((sum, row) => sum + row.revenue, 0), [rows])
  const maxValue = useMemo(
    () => Math.max(1, ...rows.map((row) => (sortBy === 'quantity' ? row.quantity : row.revenue))),
    [rows, sortBy],
  )

  const branchOptions = useMemo(
    () => [
      { value: '', label: 'Tất cả chi nhánh' },
      ...BRANCHES.map((branch) => ({ value: branch.id, label: branch.name })),
    ],
    [],
  )

  const columns = [
    { key: 'rank', header: '#', width: '52px', render: (row) => row.rank },
    { key: 'productId', header: 'Mã SP', width: '90px' },
    {
      key: 'productName',
      header: 'Tên sản phẩm',
      render: (row) => <span className="font-medium text-ink">{row.productName}</span>,
    },
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
      render: (row) => <span className="tabular-nums text-ink/60">{row.invoices}</span>,
    },
    {
      key: 'revenue',
      header: 'Doanh thu',
      align: 'right',
      render: (row) => <span className="font-semibold tabular-nums">{formatMoney(row.revenue)}</span>,
    },
    {
      key: 'share',
      header: 'Tỷ trọng',
      width: '150px',
      render: (row) => {
        const value = sortBy === 'quantity' ? row.quantity : row.revenue
        return (
          <div className="h-2 w-full overflow-hidden rounded-full bg-ink/10">
            <div
              className="h-2 rounded-full bg-accent"
              style={{ width: `${Math.max(2, (value / maxValue) * 100)}%` }}
            />
          </div>
        )
      },
    },
  ]

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Sản phẩm bán chạy</h1>
          <p className="mt-1 text-sm text-ink/60">Xếp hạng sản phẩm bán chạy nhất của chuỗi cửa hàng.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={branchFilter}
            onChange={(event) => setBranchFilter(event.target.value)}
            options={branchOptions}
            className="w-56"
            aria-label="Lọc theo chi nhánh"
          />
          <Select
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value)}
            options={SORT_OPTIONS}
            className="w-48"
            aria-label="Chọn cách xếp hạng"
          />
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
        <Spinner label="Đang tải xếp hạng sản phẩm..." />
      ) : (
        <>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard icon={Package} label="Số mã đã bán" value={rows.length} hint="Trong khoảng đang chọn" />
            <StatCard
              icon={ShoppingBag}
              label="Tổng số lượng bán"
              value={totalQuantity}
              hint="Tất cả các mã trong bảng xếp hạng"
              tone="ok"
            />
            <StatCard
              icon={Wallet}
              label="Tổng doanh thu"
              value={formatMoney(totalRevenue)}
              hint="Tương ứng với các mã đã bán"
              tone="accent"
            />
            <StatCard
              icon={Trophy}
              label="Bán chạy nhất"
              value={rows[0]?.productName ?? '—'}
              hint={rows[0] ? formatMoney(rows[0].revenue) : ''}
              tone="warn"
            />
          </div>

          <Card className="mt-4">
            <DataTable
              columns={columns}
              rows={rows}
              rowKey="productId"
              pageSize={10}
              emptyText="Khoảng thời gian này chưa có sản phẩm bán chạy"
            />
          </Card>
        </>
      )}
    </div>
  )
}
