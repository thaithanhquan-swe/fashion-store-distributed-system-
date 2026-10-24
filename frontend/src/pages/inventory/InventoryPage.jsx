import { useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  Boxes,
  PackagePlus,
  PackageX,
  TrendingDown,
  Warehouse,
} from 'lucide-react'
import {
  Badge,
  Button,
  Card,
  DataTable,
  Field,
  Input,
  Modal,
  SearchBar,
  Select,
  Spinner,
  StatCard,
} from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { BRANCHES } from '../../config/branches'
import { formatDateTime, formatMoney, STOCK_STATUS, STOCK_STATUS_LABEL } from '../../utils/format'
import * as inventoryService from '../../services/inventoryService'

// Tồn kho dùng chung cho staff, manager và admin.
// Staff và manager chỉ thấy chi nhánh của mình, admin xem toàn chuỗi.

const TONE = {
  [STOCK_STATUS.AVAILABLE]: 'ok',
  [STOCK_STATUS.LOW]: 'warn',
  [STOCK_STATUS.OUT]: 'bad',
}

const STATUS_OPTIONS = [
  { value: '', label: 'Mọi trạng thái' },
  { value: STOCK_STATUS.AVAILABLE, label: STOCK_STATUS_LABEL[STOCK_STATUS.AVAILABLE] },
  { value: STOCK_STATUS.LOW, label: STOCK_STATUS_LABEL[STOCK_STATUS.LOW] },
  { value: STOCK_STATUS.OUT, label: STOCK_STATUS_LABEL[STOCK_STATUS.OUT] },
]

// Khoá của lần tải dữ liệu, dùng để biết dữ liệu trên màn hình còn khớp bộ lọc hay không.
const buildKey = (branch, search, status, version) => `${branch ?? 'all'}|${search}|${status}|${version}`

export default function InventoryPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'admin'
  const isManager = user.role === 'manager'

  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')
  const [view, setView] = useState('all')
  const [branchFilter, setBranchFilter] = useState('')
  const [stockFilter, setStockFilter] = useState('')
  const [reload, setReload] = useState(0)
  const [result, setResult] = useState({ key: '', rows: [], summary: null, chain: null, error: '' })

  const [stockInOpen, setStockInOpen] = useState(false)
  const [stockInOptions, setStockInOptions] = useState([])
  const [stockInProduct, setStockInProduct] = useState('')
  const [stockInQuantity, setStockInQuantity] = useState('')
  const [stockInError, setStockInError] = useState('')
  const [stockInSaving, setStockInSaving] = useState(false)
  const [notice, setNotice] = useState('')

  const scopeBranch = isAdmin
    ? view === 'branch'
      ? branchFilter || null
      : null
    : (user.branchId ?? null)
  const requestKey = buildKey(scopeBranch, search, stockFilter, reload)
  const loading = result.key !== requestKey
  const rows = result.rows
  const summary = result.summary
  const chain = result.chain
  const error = result.error

  // Chờ ngừng gõ rồi mới tìm kiếm, tránh gọi API liên tục.
  useEffect(() => {
    const timer = setTimeout(() => setSearch(keyword), 300)
    return () => clearTimeout(timer)
  }, [keyword])

  useEffect(() => {
    let alive = true
    const key = buildKey(scopeBranch, search, stockFilter, reload)
    Promise.all([
      inventoryService.listInventory({
        branchId: scopeBranch,
        keyword: search,
        stockStatus: stockFilter,
      }),
      inventoryService.getInventorySummary({ branchId: scopeBranch }),
      inventoryService.getInventorySummary({ branchId: null }),
    ])
      .then(([list, stat, chain]) => {
        if (alive) setResult({ key, rows: list, summary: stat, chain, error: '' })
      })
      .catch((err) => {
        if (alive) setResult({ key, rows: [], summary: null, chain: null, error: err.message })
      })
    return () => {
      alive = false
    }
  }, [scopeBranch, search, stockFilter, reload])

  const branchOptions = useMemo(
    () =>
      view === 'branch'
        ? BRANCHES.map((branch) => ({ value: branch.id, label: branch.name }))
        : [{ value: '', label: 'Tất cả chi nhánh' }, ...BRANCHES.map((branch) => ({ value: branch.id, label: branch.name }))],
    [view],
  )

  // Chuyển sang xem theo chi nhánh thì cần có một chi nhánh được chọn.
  const changeView = (next) => {
    setView(next)
    if (next === 'branch' && !branchFilter) {
      setBranchFilter(BRANCHES[0]?.id ?? '')
    }
    if (next === 'all') {
      setBranchFilter('')
    }
  }

  const columns = useMemo(() => {
    const list = [
      { key: 'productId', header: 'Mã SP', width: '90px' },
      { key: 'productName', header: 'Tên sản phẩm' },
      { key: 'category', header: 'Phân loại' },
    ]
    if (isAdmin) {
      list.push({
        key: 'branchName',
        header: 'Chi nhánh',
        render: (row) => <span className="text-ink/70">{row.branchName}</span>,
      })
    }
    list.push(
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
        key: 'price',
        header: 'Giá bán',
        align: 'right',
        render: (row) => <span className="tabular-nums">{formatMoney(row.price)}</span>,
      },
      {
        key: 'updatedAt',
        header: 'Cập nhật',
        render: (row) => <span className="text-ink/60">{formatDateTime(row.updatedAt)}</span>,
      },
      {
        key: 'stockStatus',
        header: 'Trạng thái',
        render: (row) => <Badge tone={TONE[row.stockStatus]}>{STOCK_STATUS_LABEL[row.stockStatus]}</Badge>,
      },
    )
    return list
  }, [isAdmin])

  const openStockIn = async () => {
    setStockInProduct('')
    setStockInQuantity('')
    setStockInError('')
    // Lấy đủ mã của chi nhánh, không phụ thuộc bộ lọc đang áp dụng.
    const options = await inventoryService.listInventory({ branchId: user.branchId ?? null })
    setStockInOptions(options)
    setStockInOpen(true)
  }

  const submitStockIn = async (event) => {
    event.preventDefault()
    const amount = Number(stockInQuantity)
    if (!stockInProduct) {
      setStockInError('Chọn mã sản phẩm cần nhập')
      return
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setStockInError('Số lượng nhập phải lớn hơn 0')
      return
    }
    setStockInSaving(true)
    setStockInError('')
    try {
      const updated = await inventoryService.adjustStock({
        branchId: user.branchId,
        productId: stockInProduct,
        quantity: amount,
      })
      setNotice(`Đã nhập ${amount} sản phẩm ${updated.productName}, tồn mới là ${updated.quantity}.`)
      setStockInOpen(false)
      setReload((value) => value + 1)
    } catch (err) {
      setStockInError(err.message)
    } finally {
      setStockInSaving(false)
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Tồn kho</h1>
          <p className="mt-1 text-sm text-ink/60">
            {isAdmin
              ? 'Tồn kho toàn chuỗi và theo từng chi nhánh.'
              : `Tồn kho tại ${BRANCHES.find((b) => b.id === user.branchId)?.name ?? 'chi nhánh của bạn'}.`}
          </p>
        </div>
        {isManager && (
          <Button icon={PackagePlus} onClick={openStockIn}>
            Nhập kho
          </Button>
        )}
      </div>

      {isAdmin && (
        <div className="mt-4 inline-flex flex-wrap gap-1 rounded-lg border border-ink/10 bg-card p-1">
          {[
            { value: 'all', label: 'Tồn kho toàn chuỗi' },
            { value: 'branch', label: 'Tồn kho theo chi nhánh' },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => changeView(tab.value)}
              className={`cursor-pointer rounded-md px-3 py-1.5 text-sm font-semibold transition ${
                view === tab.value ? 'bg-accent text-white' : 'text-ink/60 hover:bg-ink/5'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {notice && (
        <div className="mt-4 flex items-center gap-2 rounded-lg bg-ok/10 px-3 py-2.5 text-sm font-medium text-ok">
          <Boxes size={16} className="shrink-0" /> {notice}
        </div>
      )}

      {error && (
        <div
          role="alert"
          className="mt-4 flex items-center gap-2 rounded-lg bg-bad/10 px-3 py-2.5 text-sm font-medium text-bad"
        >
          <AlertTriangle size={16} className="shrink-0" /> {error}
        </div>
      )}

      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={Boxes}
          label="Mã sản phẩm"
          value={summary?.totalSkus ?? 0}
          hint={`${summary?.available ?? 0} mã còn hàng`}
        />
        <StatCard
          icon={Warehouse}
          label="Tổng số lượng tồn"
          value={summary?.totalQuantity ?? 0}
          hint={`Giá trị ${formatMoney(summary?.totalValue ?? 0)}`}
          tone="accent"
        />
        <StatCard
          icon={TrendingDown}
          label="Sắp hết hàng"
          value={summary?.lowStock ?? 0}
          hint="Tồn <= mức tối thiểu"
          tone="warn"
        />
        <StatCard
          icon={PackageX}
          label="Hết hàng"
          value={summary?.outOfStock ?? 0}
          hint="Không còn sản phẩm"
          tone="bad"
        />
      </div>

      {isAdmin && view === 'all' && chain?.branchBreakdown?.length > 0 && (
        <Card title="Tồn kho toàn chuỗi" className="mt-4">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-ink/10 text-left text-[11px] font-bold uppercase tracking-wide text-ink/60">
                  <th className="px-3 pb-2">Chi nhánh</th>
                  <th className="px-3 pb-2 text-right">Số mã</th>
                  <th className="px-3 pb-2 text-right">Tổng tồn</th>
                  <th className="px-3 pb-2 text-right">Giá trị tồn</th>
                  <th className="px-3 pb-2 text-right">Sắp hết</th>
                  <th className="px-3 pb-2 text-right">Hết hàng</th>
                </tr>
              </thead>
              <tbody>
                {chain.branchBreakdown.map((branch) => (
                  <tr key={branch.branchId} className="border-b border-ink/5 last:border-0">
                    <td className="px-3 py-2.5 font-semibold text-ink">{branch.branchName}</td>
                    <td className="px-3 py-2.5 text-right tabular-nums text-ink/70">{branch.skus}</td>
                    <td className="px-3 py-2.5 text-right tabular-nums text-ink/70">{branch.quantity}</td>
                    <td className="px-3 py-2.5 text-right tabular-nums font-semibold">
                      {formatMoney(branch.value)}
                    </td>
                    <td className="px-3 py-2.5 text-right tabular-nums font-semibold text-warn">
                      {branch.lowStock}
                    </td>
                    <td className="px-3 py-2.5 text-right tabular-nums font-semibold text-bad">
                      {branch.outOfStock}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {isAdmin && view === 'branch' && chain?.branchBreakdown?.length > 0 && (
        <Card title="Tồn kho của các chi nhánh" className="mt-4">
          <div className="grid gap-3 sm:grid-cols-3">
            {chain.branchBreakdown.map((branch) => (
              <div
                key={branch.branchId}
                className={`rounded-xl border p-4 ${
                  branch.branchId === scopeBranch ? 'border-accent bg-accent/5' : 'border-ink/10'
                }`}
              >
                <p className="text-xs font-semibold text-ink/60">{branch.branchName}</p>
                <p className="mt-1 font-display text-xl font-bold text-ink">{formatMoney(branch.value)}</p>
                <p className="mt-2 text-xs text-ink/60">
                  {branch.skus} mã · tồn {branch.quantity} sản phẩm
                </p>
                <p className="mt-1 text-xs">
                  <span className="font-semibold text-warn">{branch.lowStock} sắp hết</span>
                  <span className="px-1 text-ink/30">·</span>
                  <span className="font-semibold text-bad">{branch.outOfStock} hết hàng</span>
                </p>
              </div>
            ))}
          </div>
        </Card>
      )}

      <div className="mt-4">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo mã, tên sản phẩm...">
          {isAdmin && view === 'branch' && (
            <Select
              value={branchFilter}
              onChange={(event) => setBranchFilter(event.target.value)}
              options={branchOptions}
              className="w-56"
              aria-label="Chọn chi nhánh cần xem tồn kho"
            />
          )}
          <Select
            value={stockFilter}
            onChange={(event) => setStockFilter(event.target.value)}
            options={STATUS_OPTIONS}
            className="w-48"
            aria-label="Lọc theo trạng thái tồn kho"
          />
        </SearchBar>

        {loading ? (
          <Spinner label="Đang tải tồn kho..." />
        ) : (
          <DataTable
            columns={columns}
            rows={rows}
            rowKey="key"
            pageSize={10}
            emptyText="Không có mã sản phẩm nào khớp bộ lọc"
          />
        )}
      </div>

      {stockInOpen && (
        <Modal
          title="Nhập kho"
          onClose={() => setStockInOpen(false)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setStockInOpen(false)}>
                Hủy
              </Button>
              <Button icon={PackagePlus} loading={stockInSaving} onClick={submitStockIn}>
                Nhập hàng
              </Button>
            </>
          }
        >
          <form className="space-y-4" onSubmit={submitStockIn}>
            <Field label="Mã sản phẩm" error={stockInError && !stockInProduct ? stockInError : ''}>
              <Select
                value={stockInProduct}
                onChange={(event) => setStockInProduct(event.target.value)}
                options={[
                  { value: '', label: 'Chọn sản phẩm' },
                  ...stockInOptions.map((row) => ({
                    value: row.productId,
                    label: `${row.productId} · ${row.productName} (tồn ${row.quantity})`,
                  })),
                ]}
              />
            </Field>
            <Field label="Số lượng nhập" error={stockInError && stockInProduct ? stockInError : ''}>
              <Input
                type="number"
                min="1"
                value={stockInQuantity}
                onChange={(event) => setStockInQuantity(event.target.value)}
                placeholder="Nhập số lượng"
              />
            </Field>
          </form>
        </Modal>
      )}
    </div>
  )
}
