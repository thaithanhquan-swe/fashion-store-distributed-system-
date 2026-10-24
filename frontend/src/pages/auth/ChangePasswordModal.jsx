import { useState } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Button, Field, Input, Modal } from '../../components/ui'
import * as authService from '../../services/authService'

const EMPTY = { current: '', next: '', again: '' }

// Hộp thoại đổi mật khẩu. <ChangePasswordModal onClose={() => setOpen(false)} />
export default function ChangePasswordModal({ onClose }) {
  const [form, setForm] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const validate = () => {
    const e = {}
    if (!form.current) e.current = 'Nhập mật khẩu hiện tại'
    if (form.next.length < 6) e.next = 'Mật khẩu mới tối thiểu 6 ký tự'
    else if (form.next === form.current) e.next = 'Mật khẩu mới phải khác mật khẩu hiện tại'
    if (form.again !== form.next) e.again = 'Mật khẩu nhập lại không khớp'
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
      await authService.changePassword(form.current, form.next)
      setDone(true)
    } catch (err) {
      setErrors({ current: err.message })
    } finally {
      setLoading(false)
    }
  }

  if (done) {
    return (
      <Modal title="Đổi mật khẩu" onClose={onClose} footer={<Button onClick={onClose}>Đóng</Button>}>
        <div className="flex items-center gap-3 py-2 text-ok">
          <CheckCircle2 size={22} />
          <p className="text-sm font-semibold">Đã đổi mật khẩu thành công.</p>
        </div>
      </Modal>
    )
  }

  return (
    <Modal
      title="Đổi mật khẩu"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} onClick={submit}>
            Cập nhật
          </Button>
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <Field label="Mật khẩu hiện tại" error={errors.current}>
          <Input type="password" value={form.current} onChange={set('current')} error={!!errors.current} autoComplete="current-password" autoFocus />
        </Field>
        <Field label="Mật khẩu mới" hint="Tối thiểu 6 ký tự" error={errors.next}>
          <Input type="password" value={form.next} onChange={set('next')} error={!!errors.next} autoComplete="new-password" />
        </Field>
        <Field label="Nhập lại mật khẩu mới" error={errors.again}>
          <Input type="password" value={form.again} onChange={set('again')} error={!!errors.again} autoComplete="new-password" />
        </Field>
        {/* cho phép nhấn Enter để gửi form */}
        <button type="submit" className="hidden" />
      </form>
    </Modal>
  )
}