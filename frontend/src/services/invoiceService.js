import { client, getAllPages } from '../api'
import { listCustomers } from './customerService'

// Hóa đơn lấy từ backend (/api/invoices) và thanh toán qua /api/sales/checkout.
// Backend: Admin xem toàn chuỗi, Manager/Staff chỉ thấy hóa đơn chi nhánh mình.
// Danh sách hóa đơn không có tên khách và tên nhân viên, nên tên khách được ghép từ danh sách khách hàng;
// tên nhân viên backend không trả cho mọi vai trò nên hiển thị mã nhân viên.

export const PAYMENT_METHOD = { TIEN_MAT: 'TIEN_MAT', THE: 'THE', CHUYEN_KHOAN: 'CHUYEN_KHOAN' }
export const PAYMENT_LABEL = { TIEN_MAT: 'Tiền mặt', THE: 'Thẻ', CHUYEN_KHOAN: 'Chuyển khoản' }
export const INVOICE_STATUS = { PAID: 'DA_THANH_TOAN', CANCELLED: 'DA_HUY' }


const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')

const toInvoice = (i, customerName) => ({
  invoiceId: i.invoiceId,
  branchId: i.branchId,
  customerId: i.customerId ?? null,
  customerName: customerName ?? (i.customerId ? i.customerId : 'Khách lẻ'),
  employeeId: i.employeeId,
  employeeName: i.employeeId,
  createdAt: i.createdAt,
  totalAmount: Number(i.totalAmount),
  paymentMethod: i.paymentMethod,
  status: i.status,
})

// Lọc theo chi nhánh, khách hàng, từ khóa (mã hóa đơn / tên khách), hình thức thanh toán; mới nhất trước.
export async function listInvoices({ branchId, customerId, keyword = '', method = '' } = {}) {
  const [items, customers] = await Promise.all([
    getAllPages('/api/invoices'),
    listCustomers().catch(() => []),
  ])
  const names = new Map(customers.map((c) => [c.customerId, c.fullName]))
  const q = norm(keyword.trim())
  return items
    .map((i) => toInvoice(i, names.get(i.customerId)))
    .filter(
      (i) =>
        (!branchId || i.branchId === branchId) &&
        (!customerId || i.customerId === customerId) &&
        (!method || i.paymentMethod === method) &&
        (!q || norm(i.invoiceId).includes(q) || norm(i.customerName).includes(q)),
    )
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

const toDetail = async (inv) => {
  let customerName = null
  if (inv.customerId) {
    try {
      customerName = (await client.get(`/api/customers/${inv.customerId}`)).fullName
    } catch {
      customerName = null
    }
  }
  const lines = (inv.items ?? []).map((l) => ({
    productId: l.productId,
    productName: l.productName,
    quantity: l.quantity,
    unitPrice: Number(l.unitPrice),
    discount: Number(l.discount ?? 0),
    lineTotal: Number(l.lineTotal),
  }))
  const subtotal = lines.reduce((s, l) => s + l.quantity * l.unitPrice, 0)
  return {
    ...toInvoice(inv, customerName),
    lines,
    subtotal,
    discountTotal: Math.max(0, subtotal - lines.reduce((s, l) => s + l.lineTotal, 0)),
  }
}

export async function getInvoice(invoiceId) {
  return toDetail(await client.get(`/api/invoices/${invoiceId}`))
}

// Hủy hóa đơn đã thanh toán: backend hoàn lại tồn kho và thu hồi điểm tích lũy.
export async function cancelInvoice(invoiceId) {
  return toDetail(await client.patch(`/api/invoices/${invoiceId}/cancel`))
}

// Thanh toán từ màn hình bán hàng. Backend tự lấy chi nhánh và nhân viên từ token, tự trừ kho và cộng điểm.
// items: [{productId, quantity}]. customerId để trống nghĩa là khách lẻ.
export async function checkout({ customerId, customerName, paymentMethod, items }) {
  if (!items.length) throw new Error('Giỏ hàng đang trống')
  const res = await client.post('/api/sales/checkout', {
    customerId: customerId || null,
    paymentMethod,
    items: items.map((l) => ({ productId: l.productId, quantity: l.quantity })),
  })
  return { ...toInvoice(res, customerName || undefined), loyaltyPointsEarned: res.loyaltyPointsEarned ?? 0 }
}