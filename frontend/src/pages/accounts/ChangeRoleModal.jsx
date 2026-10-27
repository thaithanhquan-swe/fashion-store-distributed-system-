import { useState } from 'react'
import { Button, Field, Modal, Select } from '../../components/ui'
import { updateAccountRole } from '../../services/accountService'

// Đổi vai trò (nhân viên <-> quản lý). Chỉ Admin dùng.
export default function ChangeRoleModal({ account, onClose, onSaved }) {
  const [role, setRole] = useState(account.role)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const save = async () => {
    setLoading(true)
    setError('')
    try {
      await updateAccountRole(account.username, role)
      onSaved()
    } catch (err) {
      setError(err.message)
      setLoading(false)
    }
  }

  return (
    <Modal
      title="Đổi vai trò"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} disabled={role === account.role} onClick={save}>
            Lưu vai trò
          </Button>
        </>
      }
    >
      <p className="mb-4 text-sm text-ink/70">
        Tài khoản <b className="text-ink">{account.username}</b> ({account.employeeName})
      </p>
      <Field label="Vai trò" hint="Quản lý xem được thống kê và quản lý nhân viên của chi nhánh">
        <Select
          value={role}
          onChange={(e) => setRole(e.target.value)}
          options={[
            { value: 'staff', label: 'Nhân viên' },
            { value: 'manager', label: 'Quản lý' },
          ]}
        />
      </Field>
      {error && <p className="mt-3 text-sm font-medium text-bad">{error}</p>}
    </Modal>
  )
}