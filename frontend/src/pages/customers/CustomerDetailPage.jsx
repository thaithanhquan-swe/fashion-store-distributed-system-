import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CircleOff, Gift, Mail, MapPin, Pencil, Phone, Power, Receipt, Wallet } from 'lucide-react'
import { Badge, Button, Card, ConfirmModal, DataTable, EmptyState, Spinner, StatCard } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDate, formatDateTime, formatMoney } from '../../utils/format'
import { getCustomer, setCustomerActive } from '../../services/customerService'
import { PAYMENT_LABEL } from '../../services/invoiceService'
import CustomerFormModal from './CustomerFormModal'

export default function CustomerDetailPage() {
  const { customerId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const canToggle = user.role === 'admin' || user.role === 'manager'
  const { data: c, loading, error, reload } = useAsyncData(() => getCustomer(customerId, { branchId: user.branchId }), [customerId, user.branchId])
  const [editing, setEditing] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [busy, setBusy] = useState(false)
  const [toggleError, setToggleError] = useState('')

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

  const toggleActive = async () => {
    setBusy(true)
    setToggleError('')
    try {
      await setCustomerActive(c.customerId, !c.active)
      setToggling(false)
      reload()
    } catch (err) {
      setToggleError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const columns = [
    {
      key: 'invoiceId',
      header: 'Hóa đơn',
      render: (i) => (
        <div className="flex items-center gap-2">
          <span className="font-semibold">{i.invoiceId}</span>
          {i.status === 'DA_HUY' && <Badge tone="bad">Đã hủy</Badge>}
        </div>
      ),
    },
    { key: 'createdAt', header: 'Thời gian', render: (i) => <span className="whitespace-nowrap">{formatDateTime(i.createdAt)}</span> },
    { key: 'branchId', header: 'Chi nhánh' },
    { key: 'paymentMethod', header: 'Thanh toán', render: (i) => <Badge>{PAYMENT_LABEL[i.paymentMethod] ?? i.paymentMethod ?? '—'}</Badge> },
    { key: 'totalAmount', header: 'Tổng tiền', align: 'right', render: (i) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(i.totalAmount)}</span> },
  ]

  return (
    <div>
      {back}
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="font-display text-2xl font-bold text-ink">{c.fullName}</h1>
            <Badge tone={c.active ? 'ok' : 'neutral'}>{c.active ? 'Hoạt động' : 'Ngừng hoạt động'}</Badge>
          </div>
          <p className="mt-1 text-sm text-ink/60">
            Mã {c.customerId}
            {c.createdAt ? ` · khách từ ${formatDate(c.createdAt)}` : ''}
          </p>
        </div>
        <div className="flex gap-2">
          {canToggle && (
            <Button variant={c.active ? 'ghost' : 'secondary'} icon={Power} onClick={() => setToggling(true)}>
              {c.active ? 'Ngừng hoạt động' : 'Kích hoạt lại'}
            </Button>
          )}
          <Button variant="secondary" icon={Pencil} onClick={() => setEditing(true)}>
            Sửa thông tin
          </Button>
        </div>
      </header>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card title="Liên hệ">
          <ul className="space-y-2.5 text-sm text-ink/80">
            <li className="flex items-center gap-2">
              <Phone size={15} className="text-ink/40" /> {c.phoneNumber || <span className="text-ink/40">Chưa có số điện thoại</span>}
            </li>
            <li className="flex items-center gap-2">
              <Mail size={15} className="text-ink/40" /> {c.email || <span className="text-ink/40">Chưa có email</span>}
            </li>
            <li className="flex items-center gap-2">
              <MapPin size={15} className="text-ink/40" /> {c.address || <span className="text-ink/40">Chưa có địa chỉ</span>}
            </li>
          </ul>
        </Card>
        <StatCard icon={Gift} label="Điểm tích lũy" value={c.loyaltyPoints} hint="Cứ 10.000 ₫ được 1 điểm" />
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

      {toggling && (
        <ConfirmModal
          title={c.active ? 'Ngừng hoạt động khách hàng?' : 'Kích hoạt lại khách hàng?'}
          message={
            toggleError ||
            (c.active
              ? `"${c.fullName}" sẽ không chọn được ở màn hình bán hàng. Lịch sử hóa đơn vẫn được giữ.`
              : `"${c.fullName}" sẽ chọn được lại ở màn hình bán hàng.`)
          }
          confirmLabel={c.active ? 'Ngừng hoạt động' : 'Kích hoạt lại'}
          danger={c.active}
          loading={busy}
          onConfirm={toggleActive}
          onClose={() => {
            setToggling(false)
            setToggleError('')
          }}
        />
      )}
    </div>
  )
}