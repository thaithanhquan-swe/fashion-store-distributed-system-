import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CircleOff, Mail, MapPin, Pencil, Phone, Receipt, Wallet } from 'lucide-react'
import { Badge, Button, Card, DataTable, EmptyState, Spinner, StatCard } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDate, formatDateTime, formatMoney } from '../../utils/format'
import { getCustomer } from '../../services/customerService'
import { PAYMENT_LABEL } from '../../services/invoiceService'
import CustomerFormModal from './CustomerFormModal'

export default function CustomerDetailPage() {
  const { customerId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: c, loading, error, reload } = useAsyncData(() => getCustomer(customerId, { branchId: user.branchId }), [customerId, user.branchId])
  const [editing, setEditing] = useState(false)

  const back = (
    <Link to={`/${user.role}/customers`} className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-ink/60 hover:text-accent">
      <ArrowLeft size={16} /> Tất cả khách hàng
    </Link>
  )

  if (error) {
    return (
      <div>
        {back}
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được khách hàng"
            description={error}
            action={
              <Button variant="secondary" onClick={reload}>
                Thử lại
              </Button>
            }
          />
        </div>
      </div>
    )
  }
  if (loading && !c) {
    return (
      <div>
        {back}
        <Spinner />
      </div>
    )
  }

  const columns = [
    { key: 'invoiceId', header: 'Hóa đơn', render: (i) => <span className="font-semibold">{i.invoiceId}</span> },
    { key: 'createdAt', header: 'Thời gian', render: (i) => <span className="whitespace-nowrap">{formatDateTime(i.createdAt)}</span> },
    { key: 'branchId', header: 'Chi nhánh' },
    { key: 'paymentMethod', header: 'Thanh toán', render: (i) => <Badge>{PAYMENT_LABEL[i.paymentMethod]}</Badge> },
    { key: 'totalAmount', header: 'Tổng tiền', align: 'right', render: (i) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(i.totalAmount)}</span> },
  ]

  return (
    <div>
      {back}
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">{c.fullName}</h1>
          <p className="mt-1 text-sm text-ink/60">
            Mã {c.customerId} · khách từ {formatDate(c.createdAt)}
          </p>
        </div>
        <Button variant="secondary" icon={Pencil} onClick={() => setEditing(true)}>
          Sửa thông tin
        </Button>
      </header>

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <Card title="Liên hệ">
          <ul className="space-y-2.5 text-sm text-ink/80">
            <li className="flex items-center gap-2">
              <Phone size={15} className="text-ink/40" /> {c.phoneNumber}
            </li>
            <li className="flex items-center gap-2">
              <Mail size={15} className="text-ink/40" /> {c.email || <span className="text-ink/40">Chưa có email</span>}
            </li>
            <li className="flex items-center gap-2">
              <MapPin size={15} className="text-ink/40" /> {c.address || <span className="text-ink/40">Chưa có địa chỉ</span>}
            </li>
          </ul>
        </Card>
        <StatCard icon={Receipt} label="Số hóa đơn" value={c.orderCount} hint={user.branchId ? `Tại ${user.branchId}` : 'Toàn chuỗi'} />
        <StatCard icon={Wallet} label="Tổng chi tiêu" value={<span className="whitespace-nowrap text-xl">{formatMoney(c.totalSpent)}</span>} hint={user.branchId ? `Tại ${user.branchId}` : 'Toàn chuỗi'} />
      </div>

      <h2 className="mb-3 font-display text-lg font-bold text-ink">Lịch sử mua hàng</h2>
      <DataTable
        columns={columns}
        rows={c.invoices}
        rowKey="invoiceId"
        emptyText="Khách hàng chưa có hóa đơn nào"
        onRowClick={(i) => navigate(`/${user.role}/invoices/${i.invoiceId}`)}
      />

      {editing && (
        <CustomerFormModal
          customer={c}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false)
            reload()
          }}
        />
      )}
    </div>
  )
}