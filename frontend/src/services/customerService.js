import { client, getAllPages } from '../api'

// Gọi API khách hàng của backend (/api/customers).
// Backend trả { customerId, fullName, phoneNumber, email, address, loyaltyPoints, registeredDate, active }.
// Khách hàng dùng chung toàn chuỗi nên mọi vai trò đều xem được cùng một danh sách.

const toCustomer = (c) => ({
  customerId: c.customerId,
  fullName: c.fullName,
  phoneNumber: c.phoneNumber ?? '',
  email: c.email ?? '',
  address: c.address ?? '',
  loyaltyPoints: c.loyaltyPoints ?? 0,
  createdAt: c.registeredDate,
  active: c.active !== false,
})

const toInvoice = (i) => ({
  invoiceId: i.invoiceId,
  branchId: i.branchId,
  customerId: i.customerId,
  employeeId: i.employeeId,
  createdAt: i.createdAt,
  totalAmount: Number(i.totalAmount),
  paymentMethod: i.paymentMethod,
  status: i.status,
})

// active = true chỉ lấy khách đang hoạt động (dùng ở màn hình bán hàng).
export async function listCustomers({ active } = {}) {
  const items = await getAllPages('/api/customers', { active })
  return items.map(toCustomer)
}

// Chi tiết khách kèm lịch sử mua hàng.
// Backend trả hóa đơn ở mọi chi nhánh; nếu truyền branchId (Staff/Manager) thì chỉ giữ hóa đơn của chi nhánh đó.
export async function getCustomer(customerId, { branchId } = {}) {
  const [customer, invoices] = await Promise.all([
    client.get(`/api/customers/${customerId}`),
    getAllPages(`/api/customers/${customerId}/invoices`),
  ])
  const history = invoices
    .map(toInvoice)
    .filter((i) => !branchId || i.branchId === branchId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  const paid = history.filter((i) => i.status === 'DA_THANH_TOAN')
  return {
    ...toCustomer(customer),
    invoices: history,
    orderCount: paid.length,
    totalSpent: paid.reduce((s, i) => s + i.totalAmount, 0),
  }
}

// Thêm mới (không có customerId) hoặc cập nhật (có customerId). Backend kiểm tra số điện thoại trùng.
export async function saveCustomer(data) {
  const body = {
    fullName: data.fullName.trim(),
    phoneNumber: data.phoneNumber.trim() || null,
    email: data.email?.trim() || null,
    address: data.address?.trim() || null,
  }
  if (!body.fullName) throw new Error('Họ tên không được để trống')

  const saved = data.customerId ? await client.put(`/api/customers/${data.customerId}`, body) : await client.post('/api/customers', body)
  return toCustomer(saved)
}

// Ngừng hoạt động hoặc kích hoạt lại khách hàng (chỉ Admin và Manager).
export async function setCustomerActive(customerId, active) {
  const saved = await client.patch(`/api/customers/${customerId}/status`, { active })
  return toCustomer(saved)
}