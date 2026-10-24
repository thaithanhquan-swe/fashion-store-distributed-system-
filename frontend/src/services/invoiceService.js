import { BRANCH_IDS, EMPLOYEES, INVOICES, INVOICE_DETAILS, wait } from './mockData'
import { loadMock, saveMock } from './mockStore'

// Dữ liệu giả cho đến khi backend có API hóa đơn.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Bám theo bảng HoaDon / ChiTietHoaDon: invoiceId, branchId, customerId, customerName, employeeId,
// createdAt, totalAmount, paymentMethod, status; dòng chi tiết: productId, productName, quantity, unitPrice, discount, lineTotal.

export const PAYMENT_METHOD = { TIEN_MAT: 'TIEN_MAT', THE: 'THE', CHUYEN_KHOAN: 'CHUYEN_KHOAN' }
export const PAYMENT_LABEL = { TIEN_MAT: 'Tiền mặt', THE: 'Thẻ', CHUYEN_KHOAN: 'Chuyển khoản' }

// Hóa đơn tạo thêm từ màn hình bán hàng được lưu riêng, ghép với dữ liệu mẫu có sẵn.
let created = loadMock('invoices-new', { invoices: [], details: [] })
const commit = (next) => {
  created = next
  saveMock('invoices-new', created)
}

const employeeName = (id) => EMPLOYEES.find((e) => e.employeeId === id)?.employeeName ?? id
const allInvoices = () => [...INVOICES, ...created.invoices]
const allDetails = () => [...INVOICE_DETAILS, ...created.details]

// Bản đồng bộ (gồm cả hóa đơn tạo ở màn POS) cho statisticsService tính doanh thu.
export const getAllInvoices = allInvoices
export const getAllInvoiceDetails = allDetails

// Lọc theo chi nhánh, khách hàng, từ khóa (mã hóa đơn / tên khách), hình thức thanh toán; mới nhất trước.
export async function listInvoices({ branchId, customerId, keyword = '', method = '' } = {}) {
  await wait(250)
  const q = keyword.trim().toLowerCase()
  return allInvoices()
    .filter(
      (i) =>
        (!branchId || i.branchId === branchId) &&
        (!customerId || i.customerId === customerId) &&
        (!method || i.paymentMethod === method) &&
        (!q || i.invoiceId.toLowerCase().includes(q) || i.customerName.toLowerCase().includes(q)),
    )
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map((i) => ({ ...i, employeeName: employeeName(i.employeeId) }))
}

export async function getInvoice(invoiceId) {
  await wait(250)
  const inv = allInvoices().find((i) => i.invoiceId === invoiceId)
  if (!inv) throw new Error('Không tìm thấy hóa đơn')
  const lines = allDetails().filter((d) => d.invoiceId === invoiceId)
  return {
    ...inv,
    employeeName: employeeName(inv.employeeId),
    lines,
    subtotal: lines.reduce((s, l) => s + l.quantity * l.unitPrice, 0),
    discountTotal: lines.reduce((s, l) => s + l.discount, 0),
  }
}

// Tạo hóa đơn mới từ màn hình bán hàng. lines: [{productId, productName, quantity, unitPrice, discount}]
export async function createInvoice({ branchId, customerId, customerName, employeeId, paymentMethod, lines }) {
  await wait(500)
  if (!lines.length) throw new Error('Giỏ hàng đang trống')
  const b = BRANCH_IDS.indexOf(branchId) + 1
  const prefix = `HD${b}`
  const next = Math.max(0, ...allInvoices().filter((i) => i.branchId === branchId).map((i) => Number(i.invoiceId.slice(3)))) + 1
  const invoiceId = `${prefix}${String(next).padStart(4, '0')}`

  const details = lines.map((l) => ({
    invoiceId,
    productId: l.productId,
    productName: l.productName,
    quantity: l.quantity,
    unitPrice: l.unitPrice,
    discount: l.discount ?? 0,
    lineTotal: l.quantity * l.unitPrice - (l.discount ?? 0),
  }))
  const invoice = {
    invoiceId,
    branchId,
    customerId: customerId ?? null,
    customerName: customerName || 'Khách lẻ',
    employeeId,
    createdAt: new Date().toISOString(),
    totalAmount: details.reduce((s, d) => s + d.lineTotal, 0),
    paymentMethod,
    status: 'DA_THANH_TOAN',
  }

  commit({ invoices: [...created.invoices, invoice], details: [...created.details, ...details] })
  return { ...invoice, employeeName: employeeName(employeeId) }
}