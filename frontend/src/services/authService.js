// Dữ liệu giả cho đến khi backend có API đăng nhập.
// Khi có API: thay thân hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
const DEMO_ACCOUNTS = [
  { username: 'staff.q1', name: 'Nguyễn Thị Mai', role: 'staff', branchId: 'CN01', employeeId: 'NV002' },
  { username: 'manager.q1', name: 'Trần Minh Khoa', role: 'manager', branchId: 'CN01', employeeId: 'NV001' },
  { username: 'admin', name: 'Ngô Thanh Tùng', role: 'admin', branchId: null, employeeId: 'NV010' },
]
const DEMO_PASSWORD = '123456'

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export async function login(username, password) {
  await wait(400)
  const acc = DEMO_ACCOUNTS.find((a) => a.username === username.trim())
  if (!acc || password !== DEMO_PASSWORD) {
    throw new Error('Sai tên đăng nhập hoặc mật khẩu')
  }
  return acc
}