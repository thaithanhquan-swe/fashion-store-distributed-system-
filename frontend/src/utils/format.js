// Định dạng tiền
export const formatMoney = (n) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(n ?? 0)) + ' ₫'

export const formatDate = (d) => new Date(d).toLocaleDateString('vi-VN')

export const formatDateTime = (d) =>
  new Date(d).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })

// Nhãn hiển thị cho mã trạng thái trả về từ API.
// Tồn kho: sắp hết khi số lượng <= mức tồn tối thiểu, hết hàng khi số lượng = 0.
export const STOCK_STATUS = {
  AVAILABLE: 'AVAILABLE',
  LOW: 'LOW',
  OUT: 'OUT',
}

export const STOCK_STATUS_LABEL = {
  AVAILABLE: 'Còn hàng',
  LOW: 'Sắp hết',
  OUT: 'Hết hàng',
}
