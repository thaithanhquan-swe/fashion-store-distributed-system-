import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleOff } from 'lucide-react'
import { Badge, Button, DataTable, EmptyState, SearchBar, Select, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDateTime, formatMoney } from '../../utils/format'
import { BRANCHES } from '../../config/branches'
import { PAYMENT_LABEL, listInvoices } from '../../services/invoiceService'

// Danh sách hóa đơn: Staff/Manager xem hóa đơn chi nhánh mình, Admin xem toàn chuỗi và lọc theo chi nhánh.
export default function InvoiceListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const isAdmin = user.role === 'admin'

  const [keyword, setKeyword] = useState('')
  const [method, setMethod] = useState('')
  const [branch, setBranch] = useState('')
  const branchId = isAdmin ? branch || null : user.branchId

  const { data, loading, error, reload } = useAsyncData(() => listInvoices({ branchId, keyword, method }), [branchId, keyword, method])

  const columns = [
    { key: 'invoiceId', header: 'Mã hóa đơn', render: (i) => ( <div className="flex items-center gap-2"> <span className="font-semibold">{i.invoiceId}</span> {i.status === 'DA_HUY' && <Badge tone="bad">Đã hủy</Badge>} </div> )},
    { key: 'createdAt', header: 'Thời gian', render: (i) => <span className="whitespace-nowrap">{formatDateTime(i.createdAt)}</span> },
    ...(isAdmin ? [{ key: 'branchId', header: 'Chi nhánh' }] : []),
    { key: 'customerName', header: 'Khách hàng', render: (i) => <span className="whitespace-nowrap">{i.customerName}</span> },
    { key: 'employeeName', header: 'Mã nhân viên', render: (i) => <span className="whitespace-nowrap">{i.employeeName}</span> },
    { key: 'paymentMethod', header: 'Thanh toán', render: (i) => <Badge>{PAYMENT_LABEL[i.paymentMethod] ?? i.paymentMethod}</Badge> },
    { key: 'totalAmount', header: 'Tổng tiền', align: 'right', render: (i) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(i.totalAmount)}</span> },
  ]

  return (
    <div>
      <header className="mb-6">
        <h1 className="font-display text-2xl font-bold text-ink">Hóa đơn</h1>
        <p className="mt-1 text-sm text-ink/60">{isAdmin ? 'Hóa đơn của toàn chuỗi.' : 'Hóa đơn bán ra tại chi nhánh của bạn.'}</p>
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo mã hóa đơn hoặc tên khách...">
        {isAdmin && (
          <Select
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            options={[{ value: '', label: 'Mọi chi nhánh' }, ...BRANCHES.map((b) => ({ value: b.id, label: b.name }))]}
          />
        )}
        <Select
          value={method}
          onChange={(e) => setMethod(e.target.value)}
          options={[{ value: '', label: 'Mọi hình thức' }, ...Object.entries(PAYMENT_LABEL).map(([value, label]) => ({ value, label }))]}
        />
      </SearchBar>

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách hóa đơn"
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
        <DataTable
          columns={columns}
          rows={data ?? []}
          rowKey="invoiceId"
          loading={loading}
          emptyText="Không có hóa đơn phù hợp"
          onRowClick={(i) => navigate(`/${user.role}/invoices/${i.invoiceId}`)}
        />
      )}
    </div>
  )
}