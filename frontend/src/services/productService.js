import { client, getAllPages } from '../api'

// Gọi API sản phẩm của backend (/api/products).
// Backend trả { productId, productName, category, brand, color, size, price, active }.
// Giao diện dùng thêm trường status ('ACTIVE' | 'INACTIVE') nên đổi ở hàm toProduct bên dưới.

export const PRODUCT_STATUS = { ACTIVE: 'ACTIVE', INACTIVE: 'INACTIVE' }
export const PRODUCT_STATUS_LABEL = { ACTIVE: 'Đang bán', INACTIVE: 'Ngừng bán' }
export const CATEGORIES = ['Áo', 'Quần', 'Váy', 'Giày', 'Phụ kiện']

const toProduct = (p) => ({
  productId: p.productId,
  productName: p.productName,
  category: p.category ?? '',
  brand: p.brand ?? '',
  color: p.color ?? '',
  size: p.size ?? '',
  price: Number(p.price),
  status: p.active ? PRODUCT_STATUS.ACTIVE : PRODUCT_STATUS.INACTIVE,
})

// Toàn bộ sản phẩm (gom hết các trang của backend).
export async function getProducts() {
  const items = await getAllPages('/api/products')
  return items.map(toProduct)
}

// Thêm mới (không có productId) hoặc cập nhật (có productId).
export async function saveProduct(data) {
  const body = {
    productName: data.productName.trim(),
    category: data.category,
    brand: data.brand.trim(),
    color: data.color.trim(),
    size: data.size.trim(),
    price: Number(data.price),
  }
  if (!body.productName) throw new Error('Tên sản phẩm không được để trống')
  if (!(body.price > 0)) throw new Error('Giá bán phải lớn hơn 0')

  const saved = data.productId
    ? await client.put(`/api/products/${data.productId}`, body)
    : await client.post('/api/products', body)
  return toProduct(saved)
}

export async function setProductStatus(productId, status) {
  const saved = await client.patch(`/api/products/${productId}/status`, { active: status === PRODUCT_STATUS.ACTIVE })
  return toProduct(saved)
}