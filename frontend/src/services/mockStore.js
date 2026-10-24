// Lưu dữ liệu giả vào localStorage để tải lại trang (F5) không bị mất.
// Chỉ dùng khi backend chưa có API; khi có API thật thì bỏ file này.
// Muốn xóa dữ liệu giả về ban đầu: chạy resetMockData() hoặc xóa các khóa bắt đầu bằng "fashion-store-mock-".
const PREFIX = 'fashion-store-mock-v1-'

export function loadMock(name, initial) {
  try {
    const raw = localStorage.getItem(PREFIX + name)
    if (raw) return JSON.parse(raw)
  } catch {
    // trình duyệt chặn lưu trữ hoặc dữ liệu hỏng: dùng dữ liệu ban đầu
  }
  return initial
}

export function saveMock(name, value) {
  try {
    localStorage.setItem(PREFIX + name, JSON.stringify(value))
  } catch {
    // bỏ qua: dữ liệu vẫn dùng được trong phiên hiện tại
  }
}

export function resetMockData() {
  try {
    Object.keys(localStorage)
      .filter((k) => k.startsWith('fashion-store-mock-'))
      .forEach((k) => localStorage.removeItem(k))
  } catch {
    // bỏ qua
  }
}