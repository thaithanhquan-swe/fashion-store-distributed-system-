import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, Receipt, Trophy, UserRoundCheck, Wallet } from 'lucide-react'
import { Card, DataTable, Select, Spinner, StatCard } from '../../components/ui'
import { BRANCHES } from '../../config/branches'
import { formatMoney } from '../../utils/format'
import * as statisticsService from '../../services/statisticsService'

// Doanh số bán hàng theo nhân viên trên toàn chuỗi.

const PERIOD_OPTIONS = [
  { value: '7', label: '7 ngày gần nhất' },
  { value: '30', label: '30 ngày gần nhất' },
  { value: '90', label: '90 ngày gần nhất' },
]

export default function EmployeeSalesPage() {
  const [days, setDays] = useState('30')
  const [branchFilter, setBranchFilter] = useState('')
  const [result, setResult] = useState({ key: '', rows: [], error: '' })

  const requestKey = `employee-sales|${days}|${branchFilter}`
  const loading = result.key !== requestKey
  const rows = result.rows
  const error = result.error

  useEffect(() => {
    let alive = true
    const key = `employee-sales|${days}|${branchFilter}`
    const period = Number(days)
    statisticsService
      .getEmployeeSales({ branchId: branchFilter || null, days: period })
      .then((list) => {
        if (alive) setResult({ key, rows: list, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, rows: [], error: err.message })
      })
    return () => {
      alive = false
    }
  }, [days, branchFilter])

  const totalRevenue = useMemo(() => rows.reduce((sum, row) => sum + row.revenue, 0), [rows])
  const totalOrders = useMemo(() => rows.reduce((sum, row) => sum + row.orders, 0), [rows])
  const maxRevenue = useMemo(() => Math.max(1, ...rows.map((row) => row.revenue)), [rows])

  const branchOptions = useMemo(
    () => [
      { value: '', label: 'Tất cả chi nhánh' },
      ...BRANCHES.map((branch) => ({ value: branch.id, label: branch.name })),
    ],
    [],
  )

  const columns = [
    { key: 'rank', header: '#', width: '52px', render: (row) => row.rank },
    { key: 'employeeId', header: 'Mã NV', width: '90px' },
    {
      key: 'employeeName',
      header: 'Họ tên',
      render: (row) => <span className="font-medium text-ink">{row.employeeName}</span>,
    },
    {
      key: 'branchName',
      header: 'Chi nhánh',
      render: (row) => <span className="text-ink/70">{row.branchName}</span>,
    },
    {
      key: 'position',
      header: 'Chức vụ',
      render: (row) => <span className="text-ink/70">{row.position}</span>,
    },
    {
      key: 'orders',
      header: 'Số hóa đơn',
      align: 'right',
      render: (row) => <span className="tabular-nums">{row.orders}</span>,
    },
    {
      key: 'items',
      header: 'Số sản phẩm',
      align: 'right',
      render: (row) => <span className="tabular-nums text-ink/60">{row.items}</span>,
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
      render: (row) => (
        <div className="h-2 w-full overflow-hidden rounded-full bg-ink/10">
          <div
            className="h-2 rounded-full bg-ok"
            style={{ width: `${Math.max(2, (row.revenue / maxRevenue) * 100)}%` }}
          />
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Doanh số nhân viên</h1>
          <p className="mt-1 text-sm text-ink/60">Xếp hạng doanh số bán hàng của nhân viên trong chuỗi.</p>
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
        <Spinner label="Đang tải doanh số nhân viên..." />
      ) : (
        <>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              icon={UserRoundCheck}
              label="Nhân viên có bán hàng"
              value={rows.length}
              hint="Trong khoảng đang chọn"
            />
            <StatCard
              icon={Receipt}
              label="Tổng số hóa đơn"
              value={totalOrders}
              hint="Của các nhân viên trong bảng"
              tone="ok"
            />
            <StatCard
              icon={Wallet}
              label="Tổng doanh thu"
              value={formatMoney(totalRevenue)}
              hint="Của các nhân viên trong bảng"
              tone="accent"
            />
            <StatCard
              icon={Trophy}
              label="Dẫn đầu doanh số"
              value={rows[0]?.employeeName ?? '—'}
              hint={rows[0] ? formatMoney(rows[0].revenue) : ''}
              tone="warn"
            />
          </div>

          <Card className="mt-4">
            <DataTable
              columns={columns}
              rows={rows}
              rowKey="employeeId"
              pageSize={10}
              emptyText="Khoảng thời gian này chưa có nhân viên nào bán hàng"
            />
          </Card>
        </>
      )}
    </div>
  )
}
