import { useState } from 'react'
import { Button, Field, Input, Modal } from '../../components/ui'
import { saveCustomer } from '../../services/customerService'

// Thêm hoặc sửa khách hàng. customer: null = thêm mới. onSaved nhận khách vừa lưu.
export default function CustomerFormModal({ customer, onClose, onSaved }) {
  const [form, setForm] = useState({
    fullName: customer?.fullName ?? '',
    phoneNumber: customer?.phoneNumber ?? '',
    email: customer?.email ?? '',
    address: customer?.address ?? '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const validate = () => {
    const e = {}
    if (!form.fullName.trim()) e.fullName = 'Nhập họ tên'
    if (!/^0\d{9}$/.test(form.phoneNumber.trim())) e.phoneNumber = 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0'
    if (form.email.trim() && !/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Email không hợp lệ'
    return e
  }

  const submit = async (ev) => {
    ev?.preventDefault()
    if (loading) return
    const e = validate()
    setErrors(e)
    if (Object.keys(e).length) return

    setLoading(true)
    setSubmitError('')
    try {
      const saved = await saveCustomer({ ...(customer ?? {}), ...form })
      onSaved(saved)
    } catch (err) {
      setSubmitError(err.message)
      setLoading(false)
    }
  }

  return (
    <Modal
      title={customer ? `Sửa khách hàng ${customer.customerId}` : 'Thêm khách hàng'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} onClick={submit}>
            {customer ? 'Lưu thay đổi' : 'Thêm khách hàng'}
          </Button>
        </>
      }
    >
      <form onSubmit={submit} className="grid gap-4">
        <Field label="Họ và tên" error={errors.fullName}>
          <Input value={form.fullName} onChange={set('fullName')} error={!!errors.fullName} autoFocus />
        </Field>
        <Field label="Số điện thoại" error={errors.phoneNumber}>
          <Input value={form.phoneNumber} onChange={set('phoneNumber')} error={!!errors.phoneNumber} inputMode="numeric" maxLength={10} />
        </Field>
        <Field label="Email (không bắt buộc)" error={errors.email}>
          <Input value={form.email} onChange={set('email')} error={!!errors.email} />
        </Field>
        <Field label="Địa chỉ (không bắt buộc)">
          <Input value={form.address} onChange={set('address')} />
        </Field>
        {submitError && <p className="text-sm font-medium text-bad">{submitError}</p>}
        <button type="submit" className="hidden" />
      </form>
    </Modal>
  )
}