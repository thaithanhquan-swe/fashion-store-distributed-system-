import { wait } from './mockData'
import { getEmployees } from './employeeService'
import { loadMock, saveMock } from './mockStore'

// Dữ liệu giả cho đến khi backend có API tài khoản.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Bám theo bảng TaiKhoan: username (TenDangNhap), employeeId (MaNV), role (VaiTro: staff/manager/admin),
// status ('ACTIVE' = hoạt động, 'LOCKED' = đã khóa), lastLogin (ISO hoặc null).
// Mỗi nhân viên có tối đa một tài khoản.

export const ACCOUNT_STATUS = { ACTIVE: 'ACTIVE', LOCKED: 'LOCKED' }
export const ACCOUNT_STATUS_LABEL = { ACTIVE: 'Hoạt động', LOCKED: 'Đã khóa' }
export const ACCOUNT_ROLES = { staff: 'Nhân viên', manager: 'Quản lý', admin: 'Quản trị viên' }

const daysAgo = (d, h = 9) => {
  const t = new Date()
  t.setDate(t.getDate() - d)
  t.setHours(h, 15, 0, 0)
  return t.toISOString()
}

const SEED = [
  { username: 'manager.q1', employeeId: 'NV001', role: 'manager', status: 'ACTIVE', lastLogin: daysAgo(0, 8) },
  { username: 'staff.q1', employeeId: 'NV002', role: 'staff', status: 'ACTIVE', lastLogin: daysAgo(0, 8) },
  { username: 'bao.q1', employeeId: 'NV003', role: 'staff', status: 'ACTIVE', lastLogin: daysAgo(1, 17) },
  { username: 'manager.q2', employeeId: 'NV004', role: 'manager', status: 'ACTIVE', lastLogin: daysAgo(0, 7) },
  { username: 'tuan.q2', employeeId: 'NV005', role: 'staff', status: 'ACTIVE', lastLogin: daysAgo(2, 18) },
  { username: 'lan.q2', employeeId: 'NV006', role: 'staff', status: 'LOCKED', lastLogin: daysAgo(30, 10) },
  { username: 'manager.q3', employeeId: 'NV007', role: 'manager', status: 'ACTIVE', lastLogin: daysAgo(1, 9) },
  { username: 'ngan.q3', employeeId: 'NV008', role: 'staff', status: 'ACTIVE', lastLogin: null },
  { username: 'admin', employeeId: 'NV010', role: 'admin', status: 'ACTIVE', lastLogin: daysAgo(0, 7) },
]

let db = loadMock('accounts', SEED)
const commit = (next) => {
  db = next
  saveMock('accounts', db)
}

// Danh sách tài khoản kèm tên nhân viên và chi nhánh. Truyền branchId để chỉ lấy một chi nhánh (Manager).
export async function getAccounts({ branchId } = {}) {
  const employees = await getEmployees({ branchId })
  await wait(200)
  const byId = new Map(employees.map((e) => [e.employeeId, e]))
  return db
    .filter((a) => byId.has(a.employeeId))
    .map((a) => ({ ...a, employeeName: byId.get(a.employeeId).employeeName, branchId: byId.get(a.employeeId).branchId }))
}

// Nhân viên đang làm nhưng chưa có tài khoản, dùng cho ô chọn khi tạo tài khoản.
export async function getEmployeesWithoutAccount({ branchId } = {}) {
  const employees = await getEmployees({ branchId })
  const hasAccount = new Set(db.map((a) => a.employeeId))
  return employees.filter((e) => e.status === 'WORKING' && !hasAccount.has(e.employeeId))
}

export async function createAccount({ employeeId, username, password, role }) {
  await wait(400)
  if (password.length < 6) throw new Error('Mật khẩu tối thiểu 6 ký tự')
  if (db.some((a) => a.username === username)) throw new Error('Tên đăng nhập đã tồn tại')
  if (db.some((a) => a.employeeId === employeeId)) throw new Error('Nhân viên này đã có tài khoản')
  const created = { username, employeeId, role, status: ACCOUNT_STATUS.ACTIVE, lastLogin: null }
  commit([...db, created])
  return created
}

export async function updateAccountRole(username, role) {
    await wait(300)
    commit(db.map((a) => (a.username === username ? { ...a, role } : a)))
}

export async function setAccountStatus(username, status) {
    await wait(300)
    commit(db.map((a) => (a.username === username ? { ...a, status } : a)))
}

// Đặt lại mật khẩu, trả về mật khẩu tạm để quản trị đưa cho nhân viên.
export async function resetAccountPassword(username) {
  await wait(400)
  if (!db.some((a) => a.username === username)) throw new Error('Không tìm thấy tài khoản')
  return `FS${Math.floor(100000 + Math.random() * 900000)}`
}