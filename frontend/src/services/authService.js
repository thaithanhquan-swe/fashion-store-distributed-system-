// Dữ liệu giả cho đến khi backend có API đăng nhập.
// Khi có API: thay thân hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
const DEMO_ACCOUNTS = [
  { username: 'staff.q1', name: 'Nguyễn Thị Mai', role: 'staff', branchId: 'CN01', employeeId: 'NV002' },
  { username: 'manager.q1', name: 'Trần Minh Khoa', role: 'manager', branchId: 'CN01', employeeId: 'NV001' },
  { username: 'admin', name: 'Ngô Thanh Tùng', role: 'admin', branchId: null, employeeId: 'NV010' },
]
let demoPassword = '123456' // giả lập mật khẩu chung, đổi được trong phiên chạy hiện tại

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export async function login(username, password) {
  await wait(400)
  const acc = DEMO_ACCOUNTS.find((a) => a.username === username.trim())
  if (!acc || password !== demoPassword) {
    throw new Error('Sai tên đăng nhập hoặc mật khẩu')
  }
  return acc
}

// Đổi mật khẩu của người đang đăng nhập. Ném Error nếu mật khẩu hiện tại sai.
export async function changePassword(currentPassword, newPassword) {
  await wait(400)
  if (currentPassword !== demoPassword) {
    throw new Error('Mật khẩu hiện tại không đúng')
  }
  demoPassword = newPassword
}