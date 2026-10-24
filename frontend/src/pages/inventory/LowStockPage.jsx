import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, PackageX, TrendingDown, Truck } from 'lucide-react'
import { Badge, Card, DataTable, SearchBar, StatCard } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { BRANCHES } from '../../config/branches'
import { STOCK_STATUS, STOCK_STATUS_LABEL } from '../../utils/format'
import * as inventoryService from '../../services/inventoryService'

// Sắp hết hàng: các mã tồn <= mức tồn tối thiểu, hết hàng khi không còn sản phẩm.
// Manager thấy cảnh báo của chi nhánh mình.

const TONE = {
  [STOCK_STATUS.AVAILABLE]: 'ok',
  [STOCK_STATUS.LOW]: 'warn',
  [STOCK_STATUS.OUT]: 'bad',
}

// Khoá của lần tải dữ liệu, dùng để biết dữ liệu trên màn hình còn khớp bộ lọc hay không.
const buildKey = (branch, search) => `${branch ?? 'all'}|${search}`

export default function LowStockPage() {
  const { user } = useAuth()
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')
  const [result, setResult] = useState({ key: '', rows: [], error: '' })

  const requestKey = buildKey(user.branchId ?? null, search)
  const loading = result.key !== requestKey
  const rows = result.rows
  const error = result.error

  useEffect(() => {
    const timer = setTimeout(() => setSearch(keyword), 300)
    return () => clearTimeout(timer)
  }, [keyword])

  useEffect(() => {
    let alive = true
    const key = buildKey(user.branchId ?? null, search)
    inventoryService
      .listLowStock({ branchId: user.branchId ?? null, keyword: search })
      .then((list) => {
        if (alive) setResult({ key, rows: list, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, rows: [], error: err.message })
      })
    return () => {
      alive = false
    }
  }, [user.branchId, search])

  const outOfStock = useMemo(() => rows.filter((row) => row.stockStatus === STOCK_STATUS.OUT), [rows])
  const lowStock = useMemo(() => rows.filter((row) => row.stockStatus === STOCK_STATUS.LOW), [rows])
  const needed = useMemo(
    () => rows.reduce((sum, row) => sum + Math.max(0, row.minQuantity - row.quantity), 0),
    [rows],
  )

  const columns = [
    { key: 'productId', header: 'Mã SP', width: '90px' },
    { key: 'productName', header: 'Tên sản phẩm' },
    { key: 'category', header: 'Phân loại' },
    {
      key: 'quantity',
      header: 'Tồn',
      align: 'right',
      render: (row) => <span className="font-semibold tabular-nums">{row.quantity}</span>,
    },
    {
      key: 'minQuantity',
      header: 'Tối thiểu',
      align: 'right',
      render: (row) => <span className="tabular-nums text-ink/60">{row.minQuantity}</span>,
    },
    {
      key: 'needed',
      header: 'Cần nhập',
      align: 'right',
      render: (row) => (
        <span className="font-semibold tabular-nums text-warn">
          {Math.max(0, row.minQuantity - row.quantity)}
        </span>
      ),
    },
    {
      key: 'stockStatus',
      header: 'Trạng thái',
      render: (row) => (
        <Badge tone={TONE[row.stockStatus]}>{STOCK_STATUS_LABEL[row.stockStatus]}</Badge>
      ),
    },
  ]

  return (
    <div>
      <h1 className="font-display text-2xl font-bold text-ink">Sắp hết hàng</h1>
      <p className="mt-1 text-sm text-ink/60">
        Cảnh báo tồn kho tại {BRANCHES.find((b) => b.id === user.branchId)?.name ?? 'chi nhánh của bạn'}.
      </p>

      {error && (
        <div
          role="alert"
          className="mt-4 flex items-center gap-2 rounded-lg bg-bad/10 px-3 py-2.5 text-sm font-medium text-bad"
        >
          <AlertTriangle size={16} className="shrink-0" /> {error}
        </div>
      )}

      <div className="mt-5 grid gap-3 sm:grid-cols-3">
        <StatCard
          icon={TrendingDown}
          label="Sắp hết hàng"
          value={lowStock.length}
          hint="Tồn <= mức tồn tối thiểu"
          tone="warn"
        />
        <StatCard
          icon={PackageX}
          label="Hết hàng"
          value={outOfStock.length}
          hint="Không còn sản phẩm"
          tone="bad"
        />
        <StatCard
          icon={Truck}
          label="Tổng số lượng cần nhập"
          value={needed}
          hint="Để đủ mức tồn tối thiểu"
          tone="accent"
        />
      </div>

      <Card className="mt-4">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm mã hoặc tên sản phẩm..." />
        <DataTable
          columns={columns}
          rows={rows}
          rowKey="key"
          pageSize={10}
          loading={loading}
          emptyText="Không có mã nào đang sắp hết"
        />
      </Card>
    </div>
  )
}
