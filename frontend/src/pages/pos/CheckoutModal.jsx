import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCircle2, UserPlus } from 'lucide-react'
import { Button, Field, Modal, Select } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatMoney } from '../../utils/format'
import { listCustomers } from '../../services/customerService'
  import { PAYMENT_LABEL, checkout } from '../../services/invoiceService'
import CustomerFormModal from '../customers/CustomerFormModal'

// Thanh toán giỏ hàng: chọn khách (hoặc khách lẻ / thêm khách mới), hình thức thanh toán, rồi tạo hóa đơn và trừ kho.
export default function CheckoutModal({ cart, total, onClose, onDone }) {
  const { user } = useAuth()
  const { data: customers, reload } = useAsyncData(() => listCustomers({ active: true }), [])
  const [customerId, setCustomerId] = useState('')
  const [method, setMethod] = useState('TIEN_MAT')
  const [adding, setAdding] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [invoice, setInvoice] = useState(null)

  const customer = customers?.find((c) => c.customerId === customerId)

    const pay = async () => {
      if (loading) return
      setLoading(true)
      setError('')
      try {
        const created = await checkout({
          customerId: customer?.customerId ?? null,
          customerName: customer?.fullName ?? '',
          paymentMethod: method,
          items: cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
        })
        setInvoice(created)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
      }

  if (invoice) {
    return (
      <Modal
        title="Thanh toán thành công"
        onClose={onDone}
        footer={
          <>
            <Link to={`/${user.role}/invoices/${invoice.invoiceId}`} className="inline-flex h-10 items-center rounded-lg px-4 text-sm font-semibold text-accent hover:underline">
              Xem hóa đơn
            </Link>
            <Button onClick={onDone}>Đơn mới</Button>
          </>
        }
      >
        <div className="flex flex-col items-center gap-2 py-4 text-center">
          <CheckCircle2 size={48} className="text-ok" />
          <p className="font-display text-lg font-bold text-ink">Hóa đơn {invoice.invoiceId}</p>
          <p className="whitespace-nowrap text-2xl font-bold tabular-nums text-ink">{formatMoney(invoice.totalAmount)}</p>
          <p className="text-sm text-ink/60">
            {invoice.customerName} · {PAYMENT_LABEL[invoice.paymentMethod]}
          </p>
        </div>
      </Modal>
    )
  }

  return (
    <>
      <Modal
        title="Thanh toán"
        onClose={loading ? () => {} : onClose}
        footer={
          <>
            <Button variant="secondary" onClick={onClose} disabled={loading}>
              Quay lại
            </Button>
            <Button loading={loading} onClick={pay}>
              Xác nhận thanh toán
            </Button>
          </>
        }
      >
        <div className="space-y-5">
          <div className="rounded-xl bg-bg p-4 text-center">
            <p className="text-sm text-ink/60">{cart.reduce((s, l) => s + l.quantity, 0)} sản phẩm · cần thanh toán</p>
            <p className="mt-1 whitespace-nowrap text-3xl font-bold tabular-nums text-ink">{formatMoney(total)}</p>
          </div>

          <Field label="Khách hàng">
            <div className="flex gap-2">
              <div className="min-w-0 flex-1">
                <Select
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  options={[
                    { value: '', label: 'Khách lẻ (không lưu)' },
                    ...(customers ?? []).map((c) => ({ value: c.customerId, label: `${c.fullName} · ${c.phoneNumber}` })),
                  ]}
                />
              </div>
              <Button variant="secondary" icon={UserPlus} title="Thêm khách mới" aria-label="Thêm khách mới" onClick={() => setAdding(true)} />
            </div>
          </Field>

          <Field label="Hình thức thanh toán">
            <div className="grid grid-cols-3 gap-2">
              {Object.entries(PAYMENT_LABEL).map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setMethod(value)}
                  className={`h-11 cursor-pointer rounded-lg border text-sm font-semibold transition ${
                    method === value ? 'border-accent bg-accent/10 text-accent' : 'border-ink/15 text-ink/70 hover:border-accent/50'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </Field>

          {error && <p className="text-sm font-medium text-bad">{error}</p>}
        </div>
      </Modal>

      {adding && (
        <CustomerFormModal
          customer={null}
          onClose={() => setAdding(false)}
          onSaved={async (saved) => {
            await reload()
            setCustomerId(saved.customerId)
            setAdding(false)
          }}
        />
      )}
    </>
  )
}