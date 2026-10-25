import { wait } from './mockData'
import { loadMock, saveMock } from './mockStore'
import { listInvoices } from './invoiceService'

// Dữ liệu giả cho đến khi backend có API khách hàng.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Bám theo bảng KhachHang (nhân bản toàn phần ở mọi site): customerId (MaKhachHang), fullName, phoneNumber, email, address, createdAt.
// Mã và tên khớp với khách trong hóa đơn mẫu của mockData.

const SEED = [
  ['KH001', 'Trần Thị Hồng Nhung', '0901000001', 'nhung.tran@mail.com', 'Quận 1, TP.HCM'],
  ['KH002', 'Lê Văn Hùng', '0901000002', 'hung.le@mail.com', 'Quận 3, TP.HCM'],
  ['KH003', 'Phạm Quốc Bảo', '0901000003', 'bao.pham@mail.com', 'Quận 7, TP.HCM'],
  ['KH004', 'Hoàng Thị Mai Anh', '0901000004', 'maianh.hoang@mail.com', 'Thủ Đức, TP.HCM'],
  ['KH005', 'Vũ Đình Trung', '0901000005', 'trung.vu@mail.com', 'Quận 7, TP.HCM'],
  ['KH006', 'Đỗ Thuỳ Linh', '0901000006', 'linh.do@mail.com', 'Quận 1, TP.HCM'],
  ['KH007', 'Ngô Gia Bách', '0901000007', 'bach.ngo@mail.com', 'Thủ Đức, TP.HCM'],
  ['KH008', 'Dương Mỹ Yến', '0901000008', 'yen.duong@mail.com', 'Quận 3, TP.HCM'],
].map(([customerId, fullName, phoneNumber, email, address], i) => ({
  customerId,
  fullName,
  phoneNumber,
  email,
  address,
  createdAt: new Date(2026, 0, 5 + i * 9).toISOString(),
}))

let db = loadMock('customers', SEED)
const commit = (next) => {
  db = next
  saveMock('customers', db)
}

// Tổng chi tiêu và số hóa đơn tính từ hóa đơn (branchId: chỉ tính ở chi nhánh đó).
export async function listCustomers({ branchId } = {}) {
  await wait(250)
  const invoices = await listInvoices({ branchId })
  const stats = new Map()
  for (const i of invoices) {
    if (!i.customerId) continue
    const s = stats.get(i.customerId) ?? { orderCount: 0, totalSpent: 0, lastPurchase: null }
    s.orderCount += 1
    s.totalSpent += i.totalAmount
    if (!s.lastPurchase || i.createdAt > s.lastPurchase) s.lastPurchase = i.createdAt
    stats.set(i.customerId, s)
  }
  return db.map((c) => ({ ...c, ...(stats.get(c.customerId) ?? { orderCount: 0, totalSpent: 0, lastPurchase: null }) }))
}

export async function getCustomer(customerId, { branchId } = {}) {
  await wait(250)
  const c = db.find((x) => x.customerId === customerId)
  if (!c) throw new Error('Không tìm thấy khách hàng')
  const invoices = await listInvoices({ branchId, customerId })
  return {
    ...c,
    invoices,
    orderCount: invoices.length,
    totalSpent: invoices.reduce((s, i) => s + i.totalAmount, 0),
  }
}

// Thêm mới (không có customerId) hoặc cập nhật (có customerId). Số điện thoại không được trùng.
export async function saveCustomer(data) {
  await wait(400)
  const fullName = data.fullName.trim()
  const phoneNumber = data.phoneNumber.trim()
  if (!fullName) throw new Error('Họ tên không được để trống')
  if (!/^0\d{9}$/.test(phoneNumber)) throw new Error('Số điện thoại gồm 10 chữ số, bắt đầu bằng 0')
  if (db.some((c) => c.phoneNumber === phoneNumber && c.customerId !== data.customerId)) {
    throw new Error('Số điện thoại này đã thuộc về một khách hàng khác')
  }
  const clean = { fullName, phoneNumber, email: data.email?.trim() ?? '', address: data.address?.trim() ?? '' }

  if (data.customerId) {
    commit(db.map((c) => (c.customerId === data.customerId ? { ...c, ...clean } : c)))
    return db.find((c) => c.customerId === data.customerId)
  }
  const next = Math.max(...db.map((c) => Number(c.customerId.slice(2)))) + 1
  const createdCustomer = { ...clean, customerId: `KH${String(next).padStart(3, '0')}`, createdAt: new Date().toISOString() }
  commit([...db, createdCustomer])
  return createdCustomer
}