import { useState } from 'react'
import { Button, Field, Input, Modal, Select } from '../../components/ui'
import { BRANCHES, getBranch } from '../../config/branches'
import { POSITIONS, saveEmployee } from '../../services/employeeService'

const GENDERS = [
  { value: 'Nam', label: 'Nam' },
  { value: 'Nữ', label: 'Nữ' },
]

const todayStr = () => new Date().toISOString().slice(0, 10)

// Thêm hoặc sửa nhân viên.
// employee: null = thêm mới. lockedBranchId: có giá trị thì cố định chi nhánh (Manager).
export default function EmployeeFormModal({ employee, lockedBranchId, onClose, onSaved }) {
  const [form, setForm] = useState({
    employeeName: employee?.employeeName ?? '',
    gender: employee?.gender ?? 'Nam',
    birthDate: employee?.birthDate ?? '',
    phone: employee?.phone ?? '',
    position: employee?.position ?? POSITIONS[0],
    branchId: employee?.branchId ?? lockedBranchId ?? BRANCHES[0].id,
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const validate = () => {
    const e = {}
    if (!form.employeeName.trim()) e.employeeName = 'Nhập họ tên'
    if (!form.birthDate) e.birthDate = 'Chọn ngày sinh'
    else if (form.birthDate > todayStr()) e.birthDate = 'Ngày sinh không hợp lệ'
    if (!/^0\d{9}$/.test(form.phone)) e.phone = 'Số điện thoại gồm 10 số, bắt đầu bằng 0'
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
      const saved = await saveEmployee({
        ...(employee ?? {}),
        ...form,
        employeeName: form.employeeName.trim(),
        branchId: lockedBranchId ?? form.branchId,
      })
      onSaved(saved, !employee)
    } catch (err) {
      setSubmitError(err.message)
      setLoading(false)
    }
  }

  return (
    <Modal
      wide
      title={employee ? `Sửa nhân viên ${employee.employeeId}` : 'Thêm nhân viên'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button loading={loading} onClick={submit}>
            {employee ? 'Lưu thay đổi' : 'Thêm nhân viên'}
          </Button>
        </>
      }
    >
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Field label="Họ và tên" error={errors.employeeName}>
            <Input value={form.employeeName} onChange={set('employeeName')} error={!!errors.employeeName} autoFocus />
          </Field>
        </div>
        <Field label="Giới tính">
          <Select value={form.gender} onChange={set('gender')} options={GENDERS} />
        </Field>
        <Field label="Ngày sinh" error={errors.birthDate}>
          <Input type="date" max={todayStr()} value={form.birthDate} onChange={set('birthDate')} error={!!errors.birthDate} />
        </Field>
        <Field label="Số điện thoại" error={errors.phone}>
          <Input value={form.phone} onChange={set('phone')} error={!!errors.phone} inputMode="numeric" maxLength={10} />
        </Field>
        <Field label="Chức vụ">
          <Select value={form.position} onChange={set('position')} options={POSITIONS.map((p) => ({ value: p, label: p }))} />
        </Field>
        <div className="sm:col-span-2">
          {lockedBranchId ? (
            <Field label="Chi nhánh" hint="Quản lý chỉ thêm nhân viên cho chi nhánh của mình">
              <Input value={getBranch(lockedBranchId)?.name ?? lockedBranchId} disabled readOnly />
            </Field>
          ) : (
            <Field label="Chi nhánh">
              <Select value={form.branchId} onChange={set('branchId')} options={BRANCHES.map((b) => ({ value: b.id, label: b.name }))} />
            </Field>
          )}
        </div>
        {submitError && <p className="text-sm font-medium text-bad sm:col-span-2">{submitError}</p>}
        <button type="submit" className="hidden" />
      </form>
    </Modal>
  )
}