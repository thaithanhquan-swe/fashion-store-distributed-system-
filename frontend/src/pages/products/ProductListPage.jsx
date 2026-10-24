import { useMemo, useState } from 'react'
import { CircleOff, Eye, EyeOff, Pencil, Plus } from 'lucide-react'
import { Badge, Button, ConfirmModal, DataTable, EmptyState, SearchBar, Select, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney, STOCK_STATUS } from '../../utils/format'
import { listInventory } from '../../services/inventoryService'
import {
  CATEGORIES,
  PRODUCT_STATUS,
  PRODUCT_STATUS_LABEL,
  getProducts,
  setProductStatus,
} from '../../services/productService'
import ProductFormModal from './ProductFormModal'

// Danh sách sản phẩm dùng chung: Staff và Manager chỉ xem (kèm tồn chi nhánh), Admin quản lý danh mục.
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

export default function ProductListPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'admin'

  const { data, loading, error, reload } = useAsyncData(async () => {
    const [products, stock] = await Promise.all([getProducts(), listInventory({ branchId: user.branchId })])
    // Cộng tồn theo sản phẩm (Admin: toàn chuỗi, chi nhánh: chỉ chi nhánh mình)
    const byProduct = new Map()
    for (const row of stock) {
      const cur = byProduct.get(row.productId) ?? { quantity: 0, stockStatus: row.stockStatus }
      cur.quantity += row.quantity
      byProduct.set(row.productId, cur)
    }
    return products.map((p) => ({ ...p, stock: byProduct.get(p.productId) ?? { quantity: 0, stockStatus: STOCK_STATUS.OUT } }))
  }, [user.branchId])

  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [form, setForm] = useState(null) // null = đóng, {} = thêm mới, {product} = sửa
  const [toggle, setToggle] = useState(null)
  const [busy, setBusy] = useState(false)

  const rows = useMemo(() => {
    const q = norm(keyword.trim())
    return (data ?? []).filter(
      (p) =>
        (!q || norm(`${p.productName} ${p.productId} ${p.brand}`).includes(q)) &&
        (!category || p.category === category) &&
        (!status || p.status === status),
    )
  }, [data, keyword, category, status])

  const changeStatus = async () => {
    setBusy(true)
    try {
      const next = toggle.status === PRODUCT_STATUS.ACTIVE ? PRODUCT_STATUS.INACTIVE : PRODUCT_STATUS.ACTIVE
      await setProductStatus(toggle.productId, next)
      setToggle(null)
      reload()
    } finally {
      setBusy(false)
    }
  }

  const columns = [
    { key: 'productId', header: 'Mã', width: 80, render: (p) => <span className="font-semibold">{p.productId}</span> },
    {
      key: 'productName',
      header: 'Sản phẩm',
      render: (p) => (
        <div>
          <p className="whitespace-nowrap font-semibold text-ink">{p.productName}</p>
          <p className="text-xs text-ink/50">
            {p.brand} · {p.color} · Size {p.size}
          </p>
        </div>
      ),
    },
    { key: 'category', header: 'Loại' },
    { key: 'price', header: 'Giá bán', align: 'right', render: (p) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(p.price)}</span> },
    {
      key: 'stock',
      header: isAdmin ? 'Tồn toàn chuỗi' : 'Tồn chi nhánh',
      align: 'right',
      render: (p) =>
        isAdmin ? (
          <span className="tabular-nums">{p.stock.quantity}</span>
        ) : (
          <div className="flex items-center justify-end gap-2">
            <span className="tabular-nums">{p.stock.quantity}</span>
            {p.stock.stockStatus === STOCK_STATUS.OUT && <Badge tone="bad">Hết hàng</Badge>}
            {p.stock.stockStatus === STOCK_STATUS.LOW && <Badge tone="warn">Sắp hết</Badge>}
          </div>
        ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (p) => <Badge tone={p.status === PRODUCT_STATUS.ACTIVE ? 'ok' : 'neutral'}>{PRODUCT_STATUS_LABEL[p.status]}</Badge>,
    },
    ...(isAdmin
      ? [
          {
            key: 'actions',
            header: '',
            align: 'right',
            render: (p) => (
              <div className="flex justify-end gap-1.5">
                <Button size="sm" variant="secondary" icon={Pencil} title="Sửa sản phẩm" aria-label="Sửa sản phẩm" onClick={() => setForm({ product: p })} />
                <Button
                  size="sm"
                  variant="ghost"
                  icon={p.status === PRODUCT_STATUS.ACTIVE ? EyeOff : Eye}
                  title={p.status === PRODUCT_STATUS.ACTIVE ? 'Ngừng bán' : 'Mở bán lại'}
                  aria-label={p.status === PRODUCT_STATUS.ACTIVE ? 'Ngừng bán' : 'Mở bán lại'}
                  onClick={() => setToggle(p)}
                />
              </div>
            ),
          },
        ]
      : []),
  ]

  const stopping = toggle?.status === PRODUCT_STATUS.ACTIVE

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Sản phẩm</h1>
          <p className="mt-1 text-sm text-ink/60">
            {isAdmin ? 'Quản lý danh mục sản phẩm dùng chung cho toàn chuỗi.' : 'Danh mục sản phẩm và tồn kho tại chi nhánh của bạn.'}
          </p>
        </div>
        {isAdmin && (
          <Button icon={Plus} onClick={() => setForm({})}>
            Thêm sản phẩm
          </Button>
        )}
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên, mã hoặc thương hiệu...">
        <Select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          options={[{ value: '', label: 'Mọi loại' }, ...CATEGORIES.map((c) => ({ value: c, label: c }))]}
        />
        <Select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          options={[
            { value: '', label: 'Mọi trạng thái' },
            { value: PRODUCT_STATUS.ACTIVE, label: PRODUCT_STATUS_LABEL.ACTIVE },
            { value: PRODUCT_STATUS.INACTIVE, label: PRODUCT_STATUS_LABEL.INACTIVE },
          ]}
        />
      </SearchBar>

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách sản phẩm"
            description={error}
            action={
              <Button variant="secondary" onClick={reload}>
                Thử lại
              </Button>
            }
          />
        </div>
      ) : loading && !data ? (
        <Spinner />
      ) : (
        <DataTable columns={columns} rows={rows} rowKey="productId" loading={loading} emptyText="Không có sản phẩm phù hợp" />
      )}

      {form && (
        <ProductFormModal
          product={form.product ?? null}
          onClose={() => setForm(null)}
          onSaved={() => {
            setForm(null)
            reload()
          }}
        />
      )}

      {toggle && (
        <ConfirmModal
          title={stopping ? 'Ngừng bán sản phẩm?' : 'Mở bán lại sản phẩm?'}
          message={
            stopping
              ? `"${toggle.productName}" sẽ không còn xuất hiện ở màn hình bán hàng. Dữ liệu hóa đơn cũ vẫn được giữ.`
              : `"${toggle.productName}" sẽ xuất hiện lại ở màn hình bán hàng.`
          }
          confirmLabel={stopping ? 'Ngừng bán' : 'Mở bán lại'}
          danger={stopping}
          loading={busy}
          onConfirm={changeStatus}
          onClose={() => setToggle(null)}
        />
      )}
    </div>
  )
}