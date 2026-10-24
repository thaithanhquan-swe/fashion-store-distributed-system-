import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CircleOff } from 'lucide-react'
import { Badge, Button, Card, DataTable, EmptyState, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDateTime, formatMoney } from '../../utils/format'
import { getBranch } from '../../config/branches'
import { PAYMENT_LABEL, getInvoice } from '../../services/invoiceService'

const Row = ({ label, children }) => (
  <div className="flex items-start justify-between gap-4 py-2 text-sm">
    <dt className="text-ink/60">{label}</dt>
    <dd className="text-right font-medium text-ink">{children}</dd>
  </div>
)

export default function InvoiceDetailPage() {
  const { invoiceId } = useParams()
  const { user } = useAuth()
  const { data: inv, loading, error, reload } = useAsyncData(async () => {
    const result = await getInvoice(invoiceId)
    // Staff/Manager chỉ xem hóa đơn của chi nhánh mình.
    if (user.branchId && result.branchId !== user.branchId) throw new Error('Hóa đơn này thuộc chi nhánh khác, bạn không có quyền xem')
    return result
  }, [invoiceId, user.branchId])

  const back = (
    <Link to={`/${user.role}/invoices`} className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-ink/60 hover:text-accent">
      <ArrowLeft size={16} /> Tất cả hóa đơn
    </Link>
  )

  if (error) {
    return (
      <div>
        {back}
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không mở được hóa đơn"
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
  if (loading && !inv) {
    return (
      <div>
        {back}
        <Spinner />
      </div>
    )
  }

  const columns = [
    { key: 'productId', header: 'Mã', width: 80 },
    { key: 'productName', header: 'Sản phẩm', render: (l) => <span className="whitespace-nowrap font-semibold">{l.productName}</span> },
    { key: 'quantity', header: 'SL', align: 'right' },
    { key: 'unitPrice', header: 'Đơn giá', align: 'right', render: (l) => <span className="whitespace-nowrap tabular-nums">{formatMoney(l.unitPrice)}</span> },
    { key: 'discount', header: 'Giảm', align: 'right', render: (l) => <span className="whitespace-nowrap tabular-nums">{l.discount ? `-${formatMoney(l.discount)}` : '—'}</span> },
    { key: 'lineTotal', header: 'Thành tiền', align: 'right', render: (l) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(l.lineTotal)}</span> },
  ]

  return (
    <div>
      {back}
      <header className="mb-6 flex flex-wrap items-center gap-3">
        <h1 className="font-display text-2xl font-bold text-ink">Hóa đơn {inv.invoiceId}</h1>
        <Badge tone="ok">Đã thanh toán</Badge>
      </header>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <DataTable columns={columns} rows={inv.lines} rowKey="productId" pageSize={20} emptyText="Hóa đơn không có dòng nào" />
        </div>
        <div className="space-y-4">
          <Card title="Thông tin">
            <dl className="divide-y divide-ink/10">
              <Row label="Thời gian">{formatDateTime(inv.createdAt)}</Row>
              <Row label="Chi nhánh">{getBranch(inv.branchId)?.name ?? inv.branchId}</Row>
              <Row label="Khách hàng">
                {inv.customerId ? (
                  <Link to={`/${user.role}/customers/${inv.customerId}`} className="text-accent hover:underline">
                    {inv.customerName}
                  </Link>
                ) : (
                  inv.customerName
                )}
              </Row>
              <Row label="Nhân viên">{inv.employeeName}</Row>
              <Row label="Thanh toán">{PAYMENT_LABEL[inv.paymentMethod]}</Row>
            </dl>
          </Card>
          <Card title="Thanh toán">
            <dl className="divide-y divide-ink/10">
              <Row label="Tạm tính">
                <span className="whitespace-nowrap tabular-nums">{formatMoney(inv.subtotal)}</span>
              </Row>
              <Row label="Giảm giá">
                <span className="whitespace-nowrap tabular-nums">{inv.discountTotal ? `-${formatMoney(inv.discountTotal)}` : '—'}</span>
              </Row>
              <Row label="Tổng cộng">
                <span className="whitespace-nowrap text-base font-bold tabular-nums">{formatMoney(inv.totalAmount)}</span>
              </Row>
            </dl>
          </Card>
        </div>
      </div>
    </div>
  )
}