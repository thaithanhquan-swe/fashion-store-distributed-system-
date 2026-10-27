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

// Sự kiện báo phiên đăng nhập hết hạn; AuthContext lắng nghe để đăng xuất và chuyển về trang đăng nhập.
export const SESSION_EXPIRED_EVENT = 'fashion-store:session-expired'

const AUTH_PATHS = ['/api/auth/login', '/api/auth/refresh_token']

let refreshing = null

// Dùng refresh token để lấy access token mới. Nhiều request cùng lúc chỉ gọi refresh một lần.
async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  if (!refreshing) {
    refreshing = fetch(`${API_URL}/api/auth/refresh_token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (res) => {
        if (!res.ok) return false
        const body = await res.json()
        const result = body?.result ?? body
        if (!result?.token) return false
        setTokens(result.token, result.refresh_token || result.refreshToken)
        return true
      })
      .catch(() => false)
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

export async function request(endpoint, options = {}, retried = false) {
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
    const isAuthCall = AUTH_PATHS.some((p) => endpoint.startsWith(p))
    if (response.status === 401 && !isAuthCall) {
      // Access token hết hạn: thử làm mới một lần, không được thì buộc đăng nhập lại.
      if (!retried && (await refreshAccessToken())) {
        return request(endpoint, options, true)
      }
      clearTokens()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      throw new Error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại')
    }
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

// Ghép tham số truy vấn, bỏ qua giá trị rỗng: buildQuery({ keyword: 'áo', active: undefined }) -> "?keyword=%C3%A1o"
export function buildQuery(params = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
  }
  const text = query.toString()
  return text ? `?${text}` : ''
}

// Backend phân trang (tối đa 100 dòng/trang). Hàm này gọi lần lượt các trang rồi trả về MỘT mảng,
// để các bảng giao diện (DataTable) tự phân trang như trước.
export async function getAllPages(path, params = {}) {
  const items = []
  for (let page = 0; page < 200; page++) {
    const res = await request(`${path}${buildQuery({ ...params, page, pageSize: 100 })}`, { method: 'GET' })
    items.push(...(res?.content ?? []))
    if (!res || res.last || !res.content?.length) break
  }
  return items
}

export const client = {
  get: (url, opts) => request(url, { ...opts, method: 'GET' }),
  post: (url, body, opts) => request(url, { ...opts, method: 'POST', body: JSON.stringify(body) }),
  put: (url, body, opts) => request(url, { ...opts, method: 'PUT', body: JSON.stringify(body) }),
  patch: (url, body, opts) => request(url, { ...opts, method: 'PATCH', body: JSON.stringify(body) }),
  delete: (url, opts) => request(url, { ...opts, method: 'DELETE' }),
}

export default client