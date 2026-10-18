import { useState } from 'react'
import { AuthContext } from './auth-context'
import * as authService from '../services/authService'

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
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // bỏ qua
    }
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}