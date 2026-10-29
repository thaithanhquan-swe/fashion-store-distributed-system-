import { useEffect, useState } from 'react'
import { AuthContext } from './auth-context'
import * as authService from '../services/authService'
import { SESSION_EXPIRED_EVENT } from '../api'

const STORAGE_KEY = 'fashion-store-user'

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadUser)

  const login = async (username, password) => {
    const u = await authService.login(username, password)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
    } catch {
      // trình duyệt chặn lưu trữ: vẫn đăng nhập được, chỉ không nhớ sau khi tải lại
    }
    setUser(u)
    return u
  }

  const logout = () => {
    authService.logout()
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // bỏ qua
    }
    setUser(null)
  }

    // Token hết hạn và không làm mới được: xóa phiên, RoleGuard tự chuyển về trang đăng nhập.
  useEffect(() => {
    const onExpired = () => {
      try {
        localStorage.removeItem(STORAGE_KEY)
      } catch {
        // bỏ qua
      }
      setUser(null)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired)
  }, [])

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}
