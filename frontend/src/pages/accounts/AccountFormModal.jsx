import { useState } from 'react'
import { Button, Field, Input, Modal, Select } from '../../components/ui'
import { getBranch } from '../../config/branches'
import { createAccount } from '../../services/accountService'

// Tạo tài khoản cho nhân viên chưa có.
// employees: nhân viên chưa có tài khoản. managerMode: Manager chỉ được tạo tài khoản nhân viên.
export default function AccountFormModal({ employees, managerMode, onClose, onSaved }) {
  const [form, setForm] = useState({
    employeeId: employees[0]?.employeeId ?? '',
    username: '',
    password: '',
    role: 'staff',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const validate = () => {
    const e = {}
    if (!form.employeeId) e.employeeId = 'Chọn nhân viên'
    if (!/^[a-zA-Z0-9._]{3,20}$/.test(form.username)) e.username = 'Nhập 3-20 ký tự: chữ không dấu, số, dấu chấm hoặc gạch dưới'
    if (form.password.length < 6) e.password = 'Mật khẩu tối thiểu 6 ký tự'
    return e
  }

  const submit = async (ev) => {
    ev?.preventDefault()
    if (loading) return
    const e = validate()
    setErrors(e)
    if (Object.keys(e).length) return

    setLoading(true)
    try {
      await createAccount({ ...form, role: managerMode ? 'staff' : form.role })
      onSaved()
    } catch (err) {
      setErrors({ username: err.message })
      setLoading(false)
    }
  }

  return (
    <Modal
      title="Tạo tài khoản"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} disabled={!employees.length} onClick={submit}>
            Tạo tài khoản
          </Button>
        </>
      }
    >
      {employees.length === 0 ? (
        <p className="text-sm text-ink/70">Tất cả nhân viên đang làm đều đã có tài khoản. Hãy thêm nhân viên mới ở trang Nhân viên trước.</p>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <Field label="Nhân viên" error={errors.employeeId}>
            <Select
              value={form.employeeId}
              onChange={set('employeeId')}
              options={employees.map((e) => ({
                value: e.employeeId,
                label: `${e.employeeId} · ${e.employeeName}${e.branchId ? ` (${getBranch(e.branchId)?.short})` : ''}`,
              }))}
            />
          </Field>
          <Field label="Tên đăng nhập" error={errors.username}>
            <Input value={form.username} onChange={set('username')} error={!!errors.username} autoComplete="off" placeholder="vd: hung.q1" />
          </Field>
          <Field label="Mật khẩu ban đầu" hint="Nhân viên nên đổi mật khẩu sau lần đăng nhập đầu" error={errors.password}>
            <Input type="text" value={form.password} onChange={set('password')} error={!!errors.password} autoComplete="off" />
          </Field>
          <Field label="Vai trò">
            <Select
              value={managerMode ? 'staff' : form.role}
              onChange={set('role')}
              disabled={managerMode}
              options={[
                { value: 'staff', label: 'Nhân viên' },
                ...(managerMode ? [] : [{ value: 'manager', label: 'Quản lý' }]),
              ]}
            />
          </Field>
          <button type="submit" className="hidden" />
        </form>
      )}
    </Modal>
  )
}