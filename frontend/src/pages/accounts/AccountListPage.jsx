import { useMemo, useState } from 'react'
import { CircleOff, Copy, KeyRound, Lock, LockOpen, Plus, UserCog } from 'lucide-react'
import { Badge, Button, ConfirmModal, DataTable, EmptyState, Modal, SearchBar, Select, Spinner } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { BRANCHES, getBranch } from '../../config/branches'

import {
  ACCOUNT_ROLES,
  ACCOUNT_STATUS,
  ACCOUNT_STATUS_LABEL,
  getAccounts,
  getEmployeesWithoutAccount,
  resetAccountPassword,
  setAccountStatus,
} from '../../services/accountService'
import AccountFormModal from './AccountFormModal'
import ChangeRoleModal from './ChangeRoleModal'

// Quản lý tài khoản, dùng chung cho Manager (chi nhánh mình, chỉ tạo/khóa/đặt lại) và Admin (toàn chuỗi).
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
const ROLE_TONE = { staff: 'neutral', manager: 'accent', admin: 'warn' }

export default function AccountListPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'admin'
  const lockedBranchId = isAdmin ? null : user.branchId

  const { data, loading, error, reload } = useAsyncData(() => getAccounts({ branchId: lockedBranchId }), [lockedBranchId])

  const [keyword, setKeyword] = useState('')
  const [branchFilter, setBranchFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  const [createList, setCreateList] = useState(null) // null = đóng, mảng = danh sách nhân viên chưa có tài khoản
  const [roleTarget, setRoleTarget] = useState(null)
  const [lockTarget, setLockTarget] = useState(null)
  const [resetTarget, setResetTarget] = useState(null)
  const [tempPassword, setTempPassword] = useState(null) // { username, password }
  const [busy, setBusy] = useState(false)

  const [actionError, setActionError] = useState('')

  const rows = useMemo(() => {
    const q = norm(keyword.trim())
    return (data ?? []).filter(
      (a) =>
        (!q || norm(`${a.username} ${a.employeeName} ${a.employeeId}`).includes(q)) &&
        (!branchFilter || a.branchId === branchFilter) &&
        (!roleFilter || a.role === roleFilter) &&
        (!statusFilter || a.status === statusFilter),
    )
  }, [data, keyword, branchFilter, roleFilter, statusFilter])

  const openCreate = async () => setCreateList(await getEmployeesWithoutAccount({ branchId: lockedBranchId }))

  const toggleLock = async () => {
    setBusy(true)
    setActionError('')
    try {
      const next = lockTarget.status === ACCOUNT_STATUS.ACTIVE ? ACCOUNT_STATUS.LOCKED : ACCOUNT_STATUS.ACTIVE
      await setAccountStatus(lockTarget.username, next)
      setLockTarget(null)
      reload()
    } catch (err) {
      setActionError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const doReset = async () => {
    setBusy(true)
    setActionError('')
    try {
      const password = await resetAccountPassword(resetTarget.username)
      setTempPassword({ username: resetTarget.username, password })
      setResetTarget(null)
    } catch (err) {
      setActionError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const columns = [
    { key: 'username', header: 'Tên đăng nhập', render: (a) => <span className="font-semibold">{a.username}</span> },
    {
      key: 'employeeName',
      header: 'Nhân viên',
      render: (a) => (
        <div>
          <p className="whitespace-nowrap font-semibold text-ink">{a.employeeName}</p>
          <p className="text-xs text-ink/50">{a.employeeId}</p>
        </div>
      ),
    },
    ...(isAdmin ? [{ key: 'branchId', header: 'Chi nhánh', render: (a) => getBranch(a.branchId)?.short ?? 'Toàn chuỗi' }] : []),
    { key: 'role', header: 'Vai trò', render: (a) => <Badge tone={ROLE_TONE[a.role]}>{ACCOUNT_ROLES[a.role]}</Badge> },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (a) => <Badge tone={a.status === ACCOUNT_STATUS.ACTIVE ? 'ok' : 'bad'}>{ACCOUNT_STATUS_LABEL[a.status]}</Badge>,
    },
    
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (a) => {
        // Không cho tự khóa mình, không đụng tới tài khoản quản trị, Manager không đụng tài khoản của quản lý khác
        const protectedRow = a.username === user.username || a.role === 'admin' || (!isAdmin && a.role === 'manager')
        if (protectedRow) return <span className="text-xs text-ink/40">{a.username === user.username ? 'Tài khoản của bạn' : ''}</span>
        const locked = a.status === ACCOUNT_STATUS.LOCKED
        return (
          <div className="flex justify-end gap-1.5">
            {isAdmin && (
              <Button size="sm" variant="secondary" icon={UserCog} title="Đổi vai trò" aria-label="Đổi vai trò" onClick={() => setRoleTarget(a)} />
            )}
            <Button size="sm" variant="secondary" icon={KeyRound} title="Đặt lại mật khẩu" aria-label="Đặt lại mật khẩu" onClick={() => setResetTarget(a)} />
            <Button
              size="sm"
              variant="ghost"
              icon={locked ? LockOpen : Lock}
              title={locked ? 'Mở khóa' : 'Khóa tài khoản'}
              aria-label={locked ? 'Mở khóa' : 'Khóa tài khoản'}
              onClick={() => setLockTarget(a)}
            />
          </div>
        )
      },
    },
  ]

  const locking = lockTarget?.status === ACCOUNT_STATUS.ACTIVE

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Tài khoản</h1>
          <p className="mt-1 text-sm text-ink/60">
            {isAdmin ? 'Tài khoản đăng nhập toàn chuỗi' : `Tài khoản của ${getBranch(lockedBranchId)?.name ?? 'chi nhánh'}`}
          </p>
        </div>
        <Button icon={Plus} onClick={openCreate}>
          Tạo tài khoản
        </Button>
      </header>

      <SearchBar value={keyword} onChange={setKeyword} placeholder="Tìm theo tên đăng nhập, tên hoặc mã nhân viên...">
        {isAdmin && (
          <Select
            value={branchFilter}
            onChange={(e) => setBranchFilter(e.target.value)}
            options={[{ value: '', label: 'Mọi chi nhánh' }, ...BRANCHES.map((b) => ({ value: b.id, label: b.short }))]}
          />
        )}
        <Select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          options={[{ value: '', label: 'Mọi vai trò' }, ...Object.entries(ACCOUNT_ROLES).map(([value, label]) => ({ value, label }))]}
        />
        <Select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={[
            { value: '', label: 'Mọi trạng thái' },
            { value: ACCOUNT_STATUS.ACTIVE, label: ACCOUNT_STATUS_LABEL.ACTIVE },
            { value: ACCOUNT_STATUS.LOCKED, label: ACCOUNT_STATUS_LABEL.LOCKED },
          ]}
        />
      </SearchBar>

      {error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            icon={CircleOff}
            title="Không tải được danh sách tài khoản"
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
        <DataTable columns={columns} rows={rows} rowKey="username" loading={loading} emptyText="Không có tài khoản phù hợp" />
      )}

      {createList && (
        <AccountFormModal
          employees={createList}
          managerMode={!isAdmin}
          onClose={() => setCreateList(null)}
          onSaved={() => {
            setCreateList(null)
            reload()
          }}
        />
      )}

      {roleTarget && (
        <ChangeRoleModal
          account={roleTarget}
          onClose={() => setRoleTarget(null)}
          onSaved={() => {
            setRoleTarget(null)
            reload()
          }}
        />
      )}

            {lockTarget && (
        <ConfirmModal
          title={locking ? 'Khóa tài khoản?' : 'Mở khóa tài khoản?'}
          message={
            actionError ||
            (locking
              ? `${lockTarget.username} (${lockTarget.employeeName}) sẽ không đăng nhập được cho đến khi được mở khóa.`
              : `${lockTarget.username} (${lockTarget.employeeName}) sẽ đăng nhập lại được bình thường.`)
          }
          confirmLabel={locking ? 'Khóa tài khoản' : 'Mở khóa'}
          danger={locking}
          loading={busy}
          onConfirm={toggleLock}
          onClose={() => {
            setLockTarget(null)
            setActionError('')
          }}
        />
      )}

      {resetTarget && (
        <ConfirmModal
          title="Đặt lại mật khẩu?"
          message={actionError || `Hệ thống tạo mật khẩu tạm mới cho ${resetTarget.username}. Mật khẩu cũ sẽ không dùng được nữa.`}
          confirmLabel="Đặt lại"
          loading={busy}
          onConfirm={doReset}
          onClose={() => {
            setResetTarget(null)
            setActionError('')
          }}
        />
      )}

      {tempPassword && (
        <Modal
          title="Mật khẩu tạm"
          onClose={() => setTempPassword(null)}
          footer={<Button onClick={() => setTempPassword(null)}>Đã ghi lại</Button>}
        >
          <p className="text-sm text-ink/70">
            Đưa mật khẩu này cho <b className="text-ink">{tempPassword.username}</b>. Mật khẩu chỉ hiện một lần, đóng hộp thoại là không xem lại được.
          </p>
          <div className="mt-4 flex items-center justify-between gap-3 rounded-lg bg-ink/5 px-4 py-3">
            <code className="text-lg font-bold tracking-wider text-ink">{tempPassword.password}</code>
            <Button
              size="sm"
              variant="secondary"
              icon={Copy}
              onClick={() => navigator.clipboard?.writeText(tempPassword.password)}
            >
              Sao chép
            </Button>
          </div>
        </Modal>
      )}
    </div>
  )
}