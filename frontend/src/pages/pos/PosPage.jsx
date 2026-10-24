import { useMemo, useState } from 'react'
import { CircleOff, Minus, Plus, ShoppingCart, Trash2 } from 'lucide-react'
import { Badge, Button, EmptyState, Select, SearchBar, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney } from '../../utils/format'
import { listInventory } from '../../services/inventoryService'
import { CATEGORIES, PRODUCT_STATUS, getProducts } from '../../services/productService'
import CheckoutModal from './CheckoutModal'

// Màn hình bán hàng (Staff, Manager): chọn sản phẩm đang bán còn hàng tại chi nhánh, lập giỏ, thanh toán.
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

export default function PosPage() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useAsyncData(async () => {
    const [products, stock] = await Promise.all([getProducts(), listInventory({ branchId: user.branchId })])
    const qty = new Map(stock.map((r) => [r.productId, r.quantity]))
    return products.filter((p) => p.status === PRODUCT_STATUS.ACTIVE).map((p) => ({ ...p, stock: qty.get(p.productId) ?? 0 }))
  }, [user.branchId])

  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [cart, setCart] = useState({}) // productId -> số lượng
  const [paying, setPaying] = useState(false)

  const products = useMemo(() => data ?? [], [data])
  const shown = useMemo(() => {
    const q = norm(keyword.trim())
    return products.filter((p) => (!q || norm(`${p.productName} ${p.productId} ${p.brand}`).includes(q)) && (!category || p.category === category))
  }, [products, keyword, category])

  const lines = products.filter((p) => cart[p.productId]).map((p) => ({ ...p, quantity: cart[p.productId] }))
  const total = lines.reduce((s, l) => s + l.quantity * l.price, 0)
  const count = lines.reduce((s, l) => s + l.quantity, 0)

  const setQty = (p, quantity) =>
    setCart((c) => {
      const next = { ...c }
      const q = Math.min(quantity, p.stock)
      if (q <= 0) delete next[p.productId]
      else next[p.productId] = q
      return next
    })

  const finish = () => {
    setPaying(false)
    setCart({})
    reload() // cập nhật lại tồn kho sau khi bán
  }

  if (error) {
    return (
      <div className="rounded-xl border border-ink/10 bg-card">
        <EmptyState
          icon={CircleOff}
          title="Không tải được dữ liệu bán hàng"
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

  return (
    <div>
      <header className="mb-6">
        <h1 className="font-display text-2xl font-bold text-ink">Bán hàng</h1>
        <p className="mt-1 text-sm text-ink/60">Chọn sản phẩm để thêm vào giỏ. Chỉ hiện sản phẩm đang bán tại chi nhánh của bạn.</p>
      </header>

      <div className="grid items-start gap-6 lg:grid-cols-[1fr_22rem]">
        <section>
          <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên, mã hoặc thương hiệu...">
            <Select value={category} onChange={(e) => setCategory(e.target.value)} options={[{ value: '', label: 'Mọi loại' }, ...CATEGORIES.map((c) => ({ value: c, label: c }))]} />
          </SearchBar>

          {loading && !data ? (
            <Spinner />
          ) : shown.length === 0 ? (
            <div className="rounded-xl border border-ink/10 bg-card">
              <EmptyState icon={ShoppingCart} title="Không có sản phẩm phù hợp" />
            </div>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {shown.map((p) => {
                const inCart = cart[p.productId] ?? 0
                const out = p.stock <= 0
                return (
                  <button
                    key={p.productId}
                    type="button"
                    disabled={out || inCart >= p.stock}
                    onClick={() => setQty(p, inCart + 1)}
                    className="cursor-pointer rounded-xl border border-ink/10 bg-card p-4 text-left transition hover:border-accent/60 hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <p className="font-semibold text-ink">{p.productName}</p>
                    <p className="mt-0.5 text-xs text-ink/50">
                      {p.brand} · {p.color} · Size {p.size}
                    </p>
                    <div className="mt-3 flex items-center justify-between gap-2">
                      <span className="whitespace-nowrap font-bold tabular-nums text-accent">{formatMoney(p.price)}</span>
                      {out ? <Badge tone="bad">Hết hàng</Badge> : <span className="whitespace-nowrap text-xs text-ink/50">Còn {p.stock}</span>}
                    </div>
                    {inCart > 0 && <p className="mt-2 text-xs font-semibold text-ok">Trong giỏ: {inCart}</p>}
                  </button>
                )
              })}
            </div>
          )}
        </section>

        <aside className="rounded-xl border border-ink/10 bg-card p-4 lg:sticky lg:top-4">
          <h2 className="mb-3 flex items-center gap-2 font-display text-lg font-bold text-ink">
            <ShoppingCart size={18} /> Giỏ hàng {count > 0 && <Badge tone="accent">{count}</Badge>}
          </h2>

          {lines.length === 0 ? (
            <p className="py-8 text-center text-sm text-ink/50">Chưa có sản phẩm nào</p>
          ) : (
            <ul className="divide-y divide-ink/10">
              {lines.map((l) => (
                <li key={l.productId} className="py-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-semibold text-ink">{l.productName}</p>
                    <button type="button" aria-label="Xóa khỏi giỏ" onClick={() => setQty(l, 0)} className="cursor-pointer text-ink/40 hover:text-bad">
                      <Trash2 size={15} />
                    </button>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Button size="sm" variant="secondary" icon={Minus} aria-label="Giảm" onClick={() => setQty(l, l.quantity - 1)} />
                      <span className="w-7 text-center text-sm font-semibold tabular-nums">{l.quantity}</span>
                      <Button size="sm" variant="secondary" icon={Plus} aria-label="Tăng" disabled={l.quantity >= l.stock} onClick={() => setQty(l, l.quantity + 1)} />
                    </div>
                    <span className="whitespace-nowrap text-sm font-semibold tabular-nums">{formatMoney(l.quantity * l.price)}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-3 border-t border-ink/10 pt-3">
            <div className="flex items-baseline justify-between">
              <span className="text-sm text-ink/60">Tổng cộng</span>
              <span className="whitespace-nowrap text-xl font-bold tabular-nums text-ink">{formatMoney(total)}</span>
            </div>
            <Button className="mt-3 w-full" disabled={lines.length === 0} onClick={() => setPaying(true)}>
              Thanh toán
            </Button>
            {lines.length > 0 && (
              <button type="button" onClick={() => setCart({})} className="mt-2 w-full cursor-pointer text-center text-xs text-ink/50 hover:text-bad">
                Xóa giỏ hàng
              </button>
            )}
          </div>
        </aside>
      </div>

      {paying && <CheckoutModal cart={lines} total={total} onClose={() => setPaying(false)} onDone={finish} />}
    </div>
  )
}