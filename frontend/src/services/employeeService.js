import { client, getAllPages } from '../api'

// Nhân viên lấy từ backend (/api/employees). Chỉ Admin và Manager gọi được; Manager chỉ thấy chi nhánh mình.
// Backend trả { employeeId, branchId, fullName, birthDate, gender, phoneNumber, email, position, active }.
// Giao diện dùng employeeName, phone và status ('WORKING' = đang làm, 'RESIGNED' = đã nghỉ).

export const EMPLOYEE_STATUS = { WORKING: 'WORKING', RESIGNED: 'RESIGNED' }
export const EMPLOYEE_STATUS_LABEL = { WORKING: 'Đang làm', RESIGNED: 'Đã nghỉ' }

export const POSITIONS = ['Nhân viên bán hàng', 'Quản lý chi nhánh']

const toEmployee = (e) => ({
  employeeId: e.employeeId,
  branchId: e.branchId ?? null,
  employeeName: e.fullName,
  birthDate: e.birthDate ?? '',
  gender: e.gender ?? '',
  phone: e.phoneNumber ?? '',
  email: e.email ?? '',
  position: e.position ?? '',
  status: e.active === false ? EMPLOYEE_STATUS.RESIGNED : EMPLOYEE_STATUS.WORKING,
})

// Lấy danh sách. Truyền branchId để chỉ lấy nhân viên của một chi nhánh (dùng cho Manager).
export async function getEmployees({ branchId } = {}) {
  const items = await getAllPages('/api/employees', { branchId })
  return items.map(toEmployee)
}

// Thêm mới (không có employeeId) hoặc cập nhật (có employeeId). Trả về nhân viên đã lưu.
export async function saveEmployee(data) {
  const body = {
    branchId: data.branchId,
    fullName: data.employeeName.trim(),
    birthDate: data.birthDate || null,
    gender: data.gender || null,
    phoneNumber: data.phone || null,
    email: data.email || null,
    position: data.position || null,
  }
  const saved = data.employeeId ? await client.put(`/api/employees/${data.employeeId}`, body) : await client.post('/api/employees', body)
  return toEmployee(saved)
}

// Cho nghỉ việc hoặc khôi phục: status là 'RESIGNED' hoặc 'WORKING'.
export async function setEmployeeStatus(employeeId, status) {
  const saved = await client.patch(`/api/employees/${employeeId}/status`, { active: status === EMPLOYEE_STATUS.WORKING })
  return toEmployee(saved)
}