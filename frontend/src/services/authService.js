import { authApi } from '../api/authApi'
import { clearTokens, setTokens } from '../api/client'

// Dịch vụ xác thực tài khoản tích hợp API backend.
export async function login(username, password) {
  const res = await authApi.login({
    username: username.trim(),
    password,
  })

  const token = res.token
  const refreshToken = res.refreshToken || res.refresh_token
  setTokens(token, refreshToken)

  const role = (res.role || '').toLowerCase()
  const name = res.fullName || res.name || res.username
  const branchId = res.branchId || null
  const employeeId = res.employeeId || null

  return {
    username: res.username,
    name,
    role,
    branchId,
    employeeId,
    accountId: res.accountId,
    token,
    refreshToken,
  }
}

// Đổi mật khẩu của người dùng hiện tại qua backend API.
export async function changePassword(currentPassword, newPassword) {
  return authApi.changePassword({
    currentPassword,
    newPassword,
  })
}

// Lấy thông tin tài khoản đang đăng nhập từ backend API.
export async function getCurrentAccount() {
  const res = await authApi.getCurrentUser()
  return {
    username: res.username,
    name: res.employeeName || res.fullName || res.username,
    role: (res.role || '').toLowerCase(),
    branchId: res.branchId || null,
    employeeId: res.employeeId || null,
    accountId: res.accountId,
    active: res.active,
  }
}

// Đăng xuất và xóa token lưu trữ.
export function logout() {
  clearTokens()
}
