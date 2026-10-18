// Định dạng tiền
export const formatMoney = (n) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(n ?? 0)) + ' ₫'

export const formatDate = (d) => new Date(d).toLocaleDateString('vi-VN')

export const formatDateTime = (d) =>
  new Date(d).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })