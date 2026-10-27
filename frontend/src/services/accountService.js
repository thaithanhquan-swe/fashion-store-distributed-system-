import { client, getAllPages } from '../api'
import { getEmployees } from './employeeService'

// Tài khoản lấy từ backend (/api/accounts). Chỉ Admin và Manager gọi được; Manager chỉ thấy chi nhánh mình.
// Backend định danh tài khoản bằng accountId (số), còn giao diện dùng username,
// nên mỗi lần tải danh sách ta nhớ cặp username -> accountId để các hàm bên dưới tra lại.
// Backend không có thông tin "đăng nhập gần nhất" nên lastLogin luôn là null.

export const ACCOUNT_STATUS = { ACTIVE: 'ACTIVE', LOCKED: 'LOCKED' }
export const ACCOUNT_STATUS_LABEL = { ACTIVE: 'Hoạt động', LOCKED: 'Đã khóa' }
export const ACCOUNT_ROLES = { staff: 'Nhân viên', manager: 'Quản lý', admin: 'Quản trị viên' }

const idByUsername = new Map()

const toAccount = (a) => {
  const role = (a.role || '').toLowerCase()
  idByUsername.set(a.username, a.accountId)
  return {
    accountId: a.accountId,
    username: a.username,
    employeeId: a.employeeId,
    employeeName: a.employeeName ?? a.employeeId,
    // Tài khoản Admin quản trị toàn chuỗi nên không gắn chi nhánh.
    branchId: role === 'admin' ? null : (a.branchId ?? null),
    role,
    status: a.active === false ? ACCOUNT_STATUS.LOCKED : ACCOUNT_STATUS.ACTIVE,
    lastLogin: null,
  }
}

const accountIdOf = (username) => {
  const id = idByUsername.get(username)
  if (id === undefined) throw new Error('Không tìm thấy tài khoản, hãy tải lại danh sách')
  return id
}

// Danh sách tài khoản kèm tên nhân viên và chi nhánh. Truyền branchId để chỉ lấy một chi nhánh (Manager).
export async function getAccounts({ branchId } = {}) {
  const items = await getAllPages('/api/accounts', { branchId })
  return items.map(toAccount)
}

// Nhân viên đang làm nhưng chưa có tài khoản, dùng cho ô chọn khi tạo tài khoản.
export async function getEmployeesWithoutAccount({ branchId } = {}) {
  const [employees, accounts] = await Promise.all([getEmployees({ branchId }), getAllPages('/api/accounts', { branchId })])
  const hasAccount = new Set(accounts.map((a) => a.employeeId))
  return employees.filter((e) => e.status === 'WORKING' && !hasAccount.has(e.employeeId))
}

export async function createAccount({ employeeId, username, password, role }) {
  if (password.length < 6) throw new Error('Mật khẩu tối thiểu 6 ký tự')
  const saved = await client.post('/api/accounts', { employeeId, username, password, role: role.toUpperCase() })
  return toAccount(saved)
}

export async function updateAccountRole(username, role) {
  const saved = await client.patch(`/api/accounts/${accountIdOf(username)}/role`, { role: role.toUpperCase() })
  return toAccount(saved)
}

export async function setAccountStatus(username, status) {
  const saved = await client.patch(`/api/accounts/${accountIdOf(username)}/status`, { active: status === ACCOUNT_STATUS.ACTIVE })
  return toAccount(saved)
}

// Đặt lại mật khẩu, trả về mật khẩu tạm để quản trị đưa cho nhân viên.
export async function resetAccountPassword(username) {
  const res = await client.patch(`/api/accounts/${accountIdOf(username)}/reset-password`, {})
  return res.tempPassword
}