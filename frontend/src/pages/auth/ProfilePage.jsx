import { useState } from 'react'
import { KeyRound } from 'lucide-react'
import { Badge, Button, Card } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { BRANCHES, getBranch } from '../../config/branches'
import { ROLE_LABEL } from '../../config/nav'
import ChangePasswordModal from './ChangePasswordModal'

// Một dòng "nhãn: giá trị" trong thẻ thông tin
function Row({ label, children }) {
  return (
    <div className="flex items-center justify-between gap-4 border-t border-ink/10 py-3 text-sm first:border-t-0 first:pt-0 last:pb-0">
      <dt className="text-ink/60">{label}</dt>
      <dd className="text-right font-semibold text-ink">{children}</dd>
    </div>
  )
}

export default function ProfilePage() {
  const { user } = useAuth()
  const [pwOpen, setPwOpen] = useState(false)
  const branch = getBranch(user.branchId)
  const initial = user.name.trim().split(' ').pop()[0]

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-2xl font-bold text-ink">Thông tin tài khoản</h1>
        <p className="mt-1 text-sm text-ink/60">Thông tin nhân viên, chi nhánh và quyền hiện tại.</p>
      </header>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <div className="mb-5 flex items-center gap-4">
            <span className="grid size-16 place-items-center rounded-full bg-accent text-2xl font-bold text-white">{initial}</span>
            <div>
              <p className="font-display text-xl font-bold text-ink">{user.name}</p>
              <Badge tone="accent">{ROLE_LABEL[user.role]}</Badge>
            </div>
          </div>
          <dl>
            <Row label="Tên đăng nhập">{user.username}</Row>
            <Row label="Mã nhân viên">{user.employeeId}</Row>
            <Row label="Vai trò">{ROLE_LABEL[user.role]}</Row>
          </dl>
        </Card>

        <div className="space-y-6">
          <Card title="Chi nhánh làm việc">
            {branch ? (
              <dl>
                <Row label="Mã chi nhánh">{branch.id}</Row>
                <Row label="Tên chi nhánh">{branch.name}</Row>
              </dl>
            ) : (
              <p className="text-sm text-ink/70">
                Tài khoản quản trị không thuộc chi nhánh nào, có quyền xem toàn chuỗi:{' '}
                {BRANCHES.map((b) => b.short).join(', ')} và Central.
              </p>
            )}
          </Card>

          <Card title="Bảo mật">
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-ink/70">Nên đổi mật khẩu định kỳ để bảo vệ tài khoản.</p>
              <Button variant="secondary" icon={KeyRound} onClick={() => setPwOpen(true)} className="shrink-0">
                Đổi mật khẩu
              </Button>
            </div>
          </Card>
        </div>
      </div>

      {pwOpen && <ChangePasswordModal onClose={() => setPwOpen(false)} />}
    </div>
  )
}