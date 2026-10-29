import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleOff, Plus } from 'lucide-react'
import { Badge, Button, DataTable, EmptyState, SearchBar, Select, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDate } from '../../utils/format'
import { listCustomers } from '../../services/customerService'
import CustomerFormModal from './CustomerFormModal'

// Danh sách khách hàng dùng chung cho cả 3 vai trò (khách hàng dùng chung toàn chuỗi).
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

export default function CustomerListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data, loading, error, reload } = useAsyncData(() => listCustomers(), [])
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [adding, setAdding] = useState(false)

  const rows = useMemo(() => {
    const q = norm(keyword.trim())
    return (data ?? []).filter(
      (c) =>
        (!q || norm(`${c.fullName} ${c.customerId} ${c.phoneNumber}`).includes(q)) &&
        (!status || (status === 'ACTIVE') === c.active),
    )
  }, [data, keyword, status])

  const columns = [
    { key: 'customerId', header: 'Mã', width: 80, render: (c) => <span className="font-semibold">{c.customerId}</span> },
    {
      key: 'fullName',
      header: 'Khách hàng',
      render: (c) => (
        <div>
          <p className="whitespace-nowrap font-semibold text-ink">{c.fullName}</p>
          <p className="text-xs text-ink/50">{c.phoneNumber || 'Chưa có số điện thoại'}</p>
        </div>
      ),
    },
    { key: 'email', header: 'Email', render: (c) => c.email || <span className="text-ink/40">—</span> },
    { key: 'loyaltyPoints', header: 'Điểm tích lũy', align: 'right', render: (c) => <span className="tabular-nums font-semibold">{c.loyaltyPoints}</span> },
    { key: 'createdAt', header: 'Ngày đăng ký', render: (c) => <span className="whitespace-nowrap">{c.createdAt ? formatDate(c.createdAt) : '—'}</span> },
    { key: 'active', header: 'Trạng thái', render: (c) => <Badge tone={c.active ? 'ok' : 'neutral'}>{c.active ? 'Hoạt động' : 'Ngừng hoạt động'}</Badge> },
  ]

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Khách hàng</h1>
          <p className="mt-1 text-sm text-ink/60">Khách hàng dùng chung toàn chuỗi.</p>
        </div>
        <Button icon={Plus} onClick={() => setAdding(true)}>
          Thêm khách hàng
        </Button>
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên, mã hoặc số điện thoại...">
        <Select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          options={[
            { value: '', label: 'Mọi trạng thái' },
            { value: 'ACTIVE', label: 'Hoạt động' },
            { value: 'INACTIVE', label: 'Ngừng hoạt động' },
          ]}
        />
      </SearchBar>

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