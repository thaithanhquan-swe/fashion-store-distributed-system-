import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleOff, Plus } from 'lucide-react'
import { Button, DataTable, EmptyState, SearchBar, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDate, formatMoney } from '../../utils/format'
import { listCustomers } from '../../services/customerService'
import CustomerFormModal from './CustomerFormModal'

// Danh sách khách hàng dùng chung cho cả 3 vai trò. Staff/manager: số liệu mua hàng tính ở chi nhánh mình.
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

export default function CustomerListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data, loading, error, reload } = useAsyncData(() => listCustomers({ branchId: user.branchId }), [user.branchId])
  const [keyword, setKeyword] = useState('')
  const [adding, setAdding] = useState(false)

  const rows = useMemo(() => {
    const q = norm(keyword.trim())
    return (data ?? []).filter((c) => !q || norm(`${c.fullName} ${c.customerId} ${c.phoneNumber}`).includes(q))
  }, [data, keyword])

  const columns = [
    { key: 'customerId', header: 'Mã', width: 80, render: (c) => <span className="font-semibold">{c.customerId}</span> },
    {
      key: 'fullName',
      header: 'Khách hàng',
      render: (c) => (
        <div>
          <p className="whitespace-nowrap font-semibold text-ink">{c.fullName}</p>
          <p className="text-xs text-ink/50">{c.phoneNumber}</p>
        </div>
      ),
    },
    { key: 'orderCount', header: 'Số hóa đơn', align: 'right', render: (c) => <span className="tabular-nums">{c.orderCount}</span> },
    { key: 'totalSpent', header: 'Tổng chi tiêu', align: 'right', render: (c) => <span className="whitespace-nowrap font-semibold tabular-nums">{formatMoney(c.totalSpent)}</span> },
    { key: 'lastPurchase', header: 'Mua gần nhất', render: (c) => (c.lastPurchase ? formatDate(c.lastPurchase) : <span className="text-ink/40">Chưa mua</span>) },
  ]

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Khách hàng</h1>
          <p className="mt-1 text-sm text-ink/60">
            {user.role === 'admin' ? 'Khách hàng dùng chung toàn chuỗi.' : 'Khách hàng dùng chung toàn chuỗi, số liệu mua hàng tính tại chi nhánh của bạn.'}
          </p>
        </div>
        <Button icon={Plus} onClick={() => setAdding(true)}>
          Thêm khách hàng
        </Button>
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên, mã hoặc số điện thoại..." />

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách khách hàng"
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
          rows={rows}
          rowKey="customerId"
          loading={loading}
          emptyText="Không có khách hàng phù hợp"
          onRowClick={(c) => navigate(`/${user.role}/customers/${c.customerId}`)}
        />
      )}

      {adding && (
        <CustomerFormModal
          customer={null}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            reload()
          }}
        />
      )}
    </div>
  )
}