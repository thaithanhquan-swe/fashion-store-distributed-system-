import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, GitCompareArrows, Store, TrendingUp, Wallet } from 'lucide-react'
import { Card, DataTable, Select, Spinner, StatCard } from '../../components/ui'
import { BRANCHES } from '../../config/branches'
import { formatDate, formatMoney } from '../../utils/format'
import * as statisticsService from '../../services/statisticsService'

// So sánh doanh thu giữa các chi nhánh trong cùng một khoảng thời gian.

const PERIOD_OPTIONS = [
  { value: '7', label: '7 ngày gần nhất' },
  { value: '14', label: '14 ngày gần nhất' },
  { value: '30', label: '30 ngày gần nhất' },
  { value: '90', label: '90 ngày gần nhất' },
]

export default function RevenueComparisonPage() {
  const [days, setDays] = useState('30')
  const [result, setResult] = useState({ key: '', branches: [], matrix: [], error: '' })

  const requestKey = `revenue-compare|${days}`
  const loading = result.key !== requestKey
  const branches = result.branches
  const matrix = result.matrix
  const error = result.error

  useEffect(() => {
    let alive = true
    const key = `revenue-compare|${days}`
    const period = Number(days)
    Promise.all([
      statisticsService.getRevenueByBranch({ days: period }),
      statisticsService.getRevenueMatrix({ days: Math.min(period, 14) }),
    ])
      .then(([branchData, matrixData]) => {
        if (alive) setResult({ key, branches: branchData, matrix: matrixData, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, branches: [], matrix: [], error: err.message })
      })
    return () => {
      alive = false
    }
  }, [days])

  const totalRevenue = useMemo(
    () => branches.reduce((sum, branch) => sum + branch.revenue, 0),
    [branches],
  )
  const maxRevenue = useMemo(() => Math.max(1, ...branches.map((b) => b.revenue)), [branches])
  const leader = branches[0]
  const gap = branches.length > 1 ? branches[0].revenue - branches[branches.length - 1].revenue : 0

  const branchColumns = [
    {
      key: 'branchName',
      header: 'Chi nhánh',
      render: (row) => <span className="font-semibold text-ink">{row.branchName}</span>,
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
    {
      key: 'averageOrderValue',
      header: 'Giá trị đơn TB',
      align: 'right',
      render: (row) => <span className="tabular-nums text-ink/70">{formatMoney(row.averageOrderValue)}</span>,
    },
    {
      key: 'share',
      header: 'Tỷ trọng',
      align: 'right',
      render: (row) => <span className="tabular-nums">{row.share}%</span>,
    },
    {
      key: 'gap',
      header: 'Chênh lệch cao nhất',
      align: 'right',
      render: (row) => (
        <span className="tabular-nums text-ink/60">
          {leader ? formatMoney(Math.max(0, leader.revenue - row.revenue)) : 0}
        </span>
      ),
    },
  ]

  const matrixColumns = useMemo(() => {
    const columns = [
      {
        key: 'dateLabel',
        header: 'Ngày',
        render: (row) => <span className="font-medium text-ink">{formatDate(row.date)}</span>,
      },
    ]
    for (const branch of BRANCHES) {
      columns.push({
        key: branch.id,
        header: branch.short,
        align: 'right',
        render: (row) => <span className="tabular-nums">{formatMoney(row[branch.id] ?? 0)}</span>,
      })
    }
    columns.push({
      key: 'total',
      header: 'Tổng',
      align: 'right',
      render: (row) => <span className="font-semibold tabular-nums">{formatMoney(row.total)}</span>,
    })
    return columns
  }, [])

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">So sánh doanh thu</h1>
          <p className="mt-1 text-sm text-ink/60">Đối chiếu doanh thu giữa các chi nhánh của chuỗi.</p>
        </div>
        <Select
          value={days}
          onChange={(event) => setDays(event.target.value)}
          options={PERIOD_OPTIONS}
          className="w-48"
          aria-label="Chọn khoảng thời gian"
        />
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
        <Spinner label="Đang so sánh doanh thu..." />
      ) : (
        <>
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            <StatCard
              icon={Wallet}
              label="Tổng doanh thu chuỗi"
              value={formatMoney(totalRevenue)}
              hint={`${branches.length} chi nhánh đang hoạt động`}
              tone="accent"
            />
            <StatCard
              icon={Store}
              label="Chi nhánh dẫn đầu"
              value={leader?.branchName ?? '—'}
              hint={leader ? formatMoney(leader.revenue) : ''}
              tone="ok"
            />
            <StatCard
              icon={TrendingUp}
              label="Chênh lệch cao nhất"
              value={formatMoney(gap)}
              hint="Giữa chi nhánh dẫn đầu và chi nhánh thấp nhất"
              tone="warn"
            />
          </div>

          <Card title="Doanh thu theo chi nhánh" className="mt-4">
            <ul className="space-y-4">
              {branches.map((branch) => (
                <li key={branch.branchId}>
                  <div className="flex items-baseline justify-between gap-3">
                    <span className="text-sm font-semibold text-ink">{branch.branchName}</span>
                    <span className="tabular-nums text-sm font-bold text-ink">
                      {formatMoney(branch.revenue)}
                    </span>
                  </div>
                  <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-ink/10">
                    <div
                      className="h-2 rounded-full bg-accent"
                      style={{ width: `${Math.max(2, (branch.revenue / maxRevenue) * 100)}%` }}
                    />
                  </div>
                  <p className="mt-1 text-xs text-ink/50">
                    {branch.orders} hóa đơn · chiếm {branch.share}% tổng doanh thu
                  </p>
                </li>
              ))}
            </ul>
          </Card>

          <Card title="Bảng so sánh" className="mt-4">
            <DataTable
              columns={branchColumns}
              rows={branches}
              rowKey="branchId"
              pageSize={8}
              emptyText="Chưa có doanh thu để so sánh"
            />
          </Card>

          <Card title="Doanh thu từng ngày theo chi nhánh" className="mt-4">
            <DataTable
              columns={matrixColumns}
              rows={matrix}
              rowKey="date"
              pageSize={8}
              emptyText="Chưa có dữ liệu theo ngày"
            />
          </Card>

          <p className="mt-4 flex items-center gap-2 text-xs text-ink/50">
            <GitCompareArrows size={14} />
            Số liệu lấy từ Central, tổng hợp từ dữ liệu của từng site.
          </p>
        </>
      )}
    </div>
  )
}
