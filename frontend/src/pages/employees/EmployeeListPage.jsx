import { useMemo, useState } from 'react'
import { CircleCheck, CircleOff, Pencil, Plus, UserRoundX } from 'lucide-react'
import { Badge, Button, ConfirmModal, DataTable, EmptyState, Select, SearchBar, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { BRANCHES, getBranch } from '../../config/branches'
import { formatDate } from '../../utils/format'
import {
  EMPLOYEE_STATUS,
  EMPLOYEE_STATUS_LABEL,
  getEmployees,
  setEmployeeStatus,
} from '../../services/employeeService'
import EmployeeFormModal from './EmployeeFormModal'

// Danh sách nhân viên, dùng chung cho Manager (chỉ chi nhánh mình) và Admin (toàn chuỗi).
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

export default function EmployeeListPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'admin'
  const lockedBranchId = isAdmin ? null : user.branchId

  const { data, loading, error, reload } = useAsyncData(() => getEmployees({ branchId: lockedBranchId }), [lockedBranchId])

  const [keyword, setKeyword] = useState('')
  const [branchFilter, setBranchFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [form, setForm] = useState(null) // null = đóng, {} = thêm mới, {employee} = sửa
  const [confirm, setConfirm] = useState(null) // nhân viên đang chờ xác nhận nghỉ / khôi phục
  const [busy, setBusy] = useState(false)

  const [actionError, setActionError] = useState('')

  const rows = useMemo(() => {
    const q = norm(keyword.trim())
    return (data ?? []).filter(
      (e) =>
        (!q || norm(`${e.employeeName} ${e.employeeId} ${e.phone}`).includes(q)) &&
        (!branchFilter || e.branchId === branchFilter) &&
        (!statusFilter || e.status === statusFilter),
    )
  }, [data, keyword, branchFilter, statusFilter])

  const changeStatus = async () => {
    setBusy(true)
    setActionError('')
    const next = confirm.status === EMPLOYEE_STATUS.WORKING ? EMPLOYEE_STATUS.RESIGNED : EMPLOYEE_STATUS.WORKING
    try {
      await setEmployeeStatus(confirm.employeeId, next)
      setConfirm(null)
      reload()
    } catch (err) {
      setActionError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const columns = [
    { key: 'employeeId', header: 'Mã NV', width: 90, render: (e) => <span className="font-semibold">{e.employeeId}</span> },
    {
      key: 'employeeName',
      header: 'Nhân viên',
      render: (e) => (
        <div>
          <p className="whitespace-nowrap font-semibold text-ink">{e.employeeName}</p>
          <p className="text-xs text-ink/50">{e.phone}</p>
        </div>
      ),
    },
    { key: 'gender', header: 'Giới tính' },
    { key: 'birthDate', header: 'Ngày sinh', render: (e) => formatDate(e.birthDate) },
    { key: 'position', header: 'Chức vụ', render: (e) => <span className="whitespace-nowrap">{e.position}</span> },
    ...(isAdmin
      ? [{ key: 'branchId', header: 'Chi nhánh', render: (e) => getBranch(e.branchId)?.short ?? 'Toàn chuỗi' }]
      : []),
    {
      key: 'status',
      header: 'Trạng thái',
      render: (e) => (
        <Badge tone={e.status === EMPLOYEE_STATUS.WORKING ? 'ok' : 'neutral'}>{EMPLOYEE_STATUS_LABEL[e.status]}</Badge>
      ),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (e) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="secondary" icon={Pencil} title="Sửa thông tin" aria-label="Sửa thông tin" onClick={() => setForm({ employee: e })} />
          {e.employeeId !== user.employeeId && (
            <Button
              size="sm"
              variant="ghost"
              icon={e.status === EMPLOYEE_STATUS.WORKING ? UserRoundX : CircleCheck}
              title={e.status === EMPLOYEE_STATUS.WORKING ? 'Cho nghỉ việc' : 'Khôi phục'}
              aria-label={e.status === EMPLOYEE_STATUS.WORKING ? 'Cho nghỉ việc' : 'Khôi phục'}
              onClick={() => setConfirm(e)}
            />
          )}
        </div>
      ),
    },
  ]

  const resigning = confirm?.status === EMPLOYEE_STATUS.WORKING

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Nhân viên</h1>
          <p className="mt-1 text-sm text-ink/60">
            {isAdmin ? 'Nhân viên toàn chuỗi' : `Nhân viên của ${getBranch(lockedBranchId)?.name ?? 'chi nhánh'}`}
          </p>
        </div>
        <Button icon={Plus} onClick={() => setForm({})}>
          Thêm nhân viên
        </Button>
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên, mã hoặc số điện thoại...">
        {isAdmin && (
          <Select
            value={branchFilter}
            onChange={(e) => setBranchFilter(e.target.value)}
            options={[{ value: '', label: 'Mọi chi nhánh' }, ...BRANCHES.map((b) => ({ value: b.id, label: b.short }))]}
          />
        )}
        <Select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={[
            { value: '', label: 'Mọi trạng thái' },
            { value: EMPLOYEE_STATUS.WORKING, label: EMPLOYEE_STATUS_LABEL.WORKING },
            { value: EMPLOYEE_STATUS.RESIGNED, label: EMPLOYEE_STATUS_LABEL.RESIGNED },
          ]}
        />
      </SearchBar>

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách nhân viên"
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
          rowKey="employeeId"
          loading={loading}
          emptyText="Không có nhân viên phù hợp"
        />
      )}

      {form && (
        <EmployeeFormModal
          employee={form.employee ?? null}
          lockedBranchId={lockedBranchId}
          onClose={() => setForm(null)}
          onSaved={() => {
            setForm(null)
            reload()
          }}
        />
      )}

      {confirm && (
        <ConfirmModal
          title={resigning ? 'Cho nhân viên nghỉ việc?' : 'Khôi phục nhân viên?'}
          message={
            actionError ||
            (resigning
              ? `${confirm.employeeName} (${confirm.employeeId}) sẽ chuyển sang trạng thái "Đã nghỉ". Dữ liệu hóa đơn cũ vẫn được giữ.`
              : `${confirm.employeeName} (${confirm.employeeId}) sẽ quay lại trạng thái "Đang làm".`)
          }
          confirmLabel={resigning ? 'Cho nghỉ việc' : 'Khôi phục'}
          danger={resigning}
          loading={busy}
          onConfirm={changeStatus}
          onClose={() => {
            setConfirm(null)
            setActionError('')
          }}
        />
      )}
    </div>
  )
}