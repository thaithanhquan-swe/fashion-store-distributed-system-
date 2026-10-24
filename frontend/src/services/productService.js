import { PRODUCTS, wait } from './mockData'
import { loadMock, saveMock } from './mockStore'

// Dữ liệu giả cho đến khi backend có API sản phẩm.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Bám theo bảng SanPham (nhân bản toàn phần ở mọi site): productId (MaSanPham), productName (TenSanPham),
// category, brand (ThuongHieu), color (MauSac), size (KichThuoc), price (GiaBan), status (TrangThai).
// status: 'ACTIVE' = đang bán, 'INACTIVE' = ngừng bán.

export const PRODUCT_STATUS = { ACTIVE: 'ACTIVE', INACTIVE: 'INACTIVE' }
export const PRODUCT_STATUS_LABEL = { ACTIVE: 'Đang bán', INACTIVE: 'Ngừng bán' }
export const CATEGORIES = ['Áo', 'Quần', 'Váy', 'Giày', 'Phụ kiện']

let db = loadMock(
  'products',
  PRODUCTS.map((p) => ({ ...p, status: PRODUCT_STATUS.ACTIVE })),
)
const commit = (next) => {
  db = next
  saveMock('products', db)
}

export async function getProducts() {
  await wait(250)
  return db.map((p) => ({ ...p }))
}

// Thêm mới (không có productId) hoặc cập nhật (có productId).
export async function saveProduct(data) {
  await wait(400)
  if (!data.productName.trim()) throw new Error('Tên sản phẩm không được để trống')
  if (!(Number(data.price) > 0)) throw new Error('Giá bán phải lớn hơn 0')
  const clean = { ...data, productName: data.productName.trim(), price: Number(data.price) }

  if (clean.productId) {
    commit(db.map((p) => (p.productId === clean.productId ? { ...p, ...clean } : p)))
    return db.find((p) => p.productId === clean.productId)
  }
  const next = Math.max(...db.map((p) => Number(p.productId.slice(2)))) + 1
  const created = { ...clean, productId: `SP${String(next).padStart(3, '0')}`, status: PRODUCT_STATUS.ACTIVE }
  commit([...db, created])
  return created
}

export async function setProductStatus(productId, status) {
  await wait(300)
  commit(db.map((p) => (p.productId === productId ? { ...p, status } : p)))
}