import { API_URL } from '../config/env'

const TOKEN_KEY = 'fashion-store-token'
const REFRESH_TOKEN_KEY = 'fashion-store-refresh-token'
const USER_KEY = 'fashion-store-user'

export const getToken = () => {
  try {
    const direct = localStorage.getItem(TOKEN_KEY)
    if (direct) return direct
    const userJson = localStorage.getItem(USER_KEY)
    if (userJson) {
      const user = JSON.parse(userJson)
      return user?.token || null
    }
  } catch {
    return null
  }
  return null
}

export const getRefreshToken = () => {
  try {
    const direct = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (direct) return direct
    const userJson = localStorage.getItem(USER_KEY)
    if (userJson) {
      const user = JSON.parse(userJson)
      return user?.refreshToken || user?.refresh_token || null
    }
  } catch {
    return null
  }
  return null
}

export const setTokens = (token, refreshToken) => {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } catch {
    // ignore
  }
}

export const clearTokens = () => {
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch {
    // ignore
  }
}

export async function request(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_URL}${endpoint}`
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  const token = getToken()
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(url, {
      ...options,
      headers,
    })
  } catch (err) {
    throw new Error('Không thể kết nối đến máy chủ backend (hãy kiểm tra xem backend đã chạy ở cổng 8080 chưa)', {
      cause: err,
    })
  }

  let data = null
  const text = await response.text()
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!response.ok) {
    const message = data?.message || (response.status === 401 ? 'Tên đăng nhập hoặc mật khẩu không đúng' : `Yêu cầu thất bại (${response.status})`)
    const error = new Error(message)
    error.status = response.status
    error.data = data
    throw error
  }

  if (data && typeof data === 'object' && 'result' in data) {
    return data.result
  }

  return data
}

export const client = {
  get: (url, opts) => request(url, { ...opts, method: 'GET' }),
  post: (url, body, opts) => request(url, { ...opts, method: 'POST', body: JSON.stringify(body) }),
  put: (url, body, opts) => request(url, { ...opts, method: 'PUT', body: JSON.stringify(body) }),
  patch: (url, body, opts) => request(url, { ...opts, method: 'PATCH', body: JSON.stringify(body) }),
  delete: (url, opts) => request(url, { ...opts, method: 'DELETE' }),
}

export default client
