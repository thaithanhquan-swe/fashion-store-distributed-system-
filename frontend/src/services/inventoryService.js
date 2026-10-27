import { BRANCHES, getBranch } from '../config/branches'
import { STOCK_STATUS } from '../utils/format'
import { client, getAllPages } from '../api'
import { getProducts } from './productService'

// Tồn kho lấy từ backend (/api/inventory).
// Backend chỉ trả { branchId, productId, quantity, minimumStock, updatedAt, lowStock },
// nên tên sản phẩm, danh mục, giá... được ghép từ danh sách sản phẩm.

// Sắp hết khi số lượng tồn <= mức tồn tối thiểu, hết hàng khi không còn sản phẩm.
const stockStatusOf = (quantity, minQuantity) => {
  if (quantity <= 0) return STOCK_STATUS.OUT
  if (quantity <= minQuantity) return STOCK_STATUS.LOW
  return STOCK_STATUS.AVAILABLE
}

const toRow = (row, product) => ({
  key: `${row.branchId}-${row.productId}`,
  branchId: row.branchId,
  branchName: getBranch(row.branchId)?.name ?? row.branchId,
  productId: row.productId,
  productName: product?.productName ?? row.productId,
  category: product?.category ?? '',
  brand: product?.brand ?? '',
  color: product?.color ?? '',
  size: product?.size ?? '',
  price: product?.price ?? 0,
  quantity: row.quantity,
  minQuantity: row.minimumStock,
  updatedAt: row.updatedAt,
  stockStatus: stockStatusOf(row.quantity, row.minimumStock),
})

// Một màn hình có thể gọi nhiều hàm cùng lúc; giữ kết quả 2 giây để không gọi lại API nhiều lần.
const CACHE_MS = 2000
let cache = null

async function loadAllRows() {
  if (cache && Date.now() - cache.at < CACHE_MS) return cache.promise
  const promise = (async () => {
    const [products, ...perBranch] = await Promise.all([
      getProducts(),
      ...BRANCHES.map((b) => getAllPages(`/api/inventory/branches/${b.id}`)),
    ])
    const productMap = new Map(products.map((p) => [p.productId, p]))
    return perBranch.flat().map((row) => toRow(row, productMap.get(row.productId)))
  })()
  cache = { at: Date.now(), promise }
  promise.catch(() => {
    if (cache?.promise === promise) cache = null
  })
  return promise
}

const matches = (row, keyword) => {
  if (!keyword) return true
  return (
    row.productId.toLowerCase().includes(keyword) ||
    row.productName.toLowerCase().includes(keyword) ||
    row.category.toLowerCase().includes(keyword) ||
    row.brand.toLowerCase().includes(keyword)
  )
}

// Danh sách tồn kho. branchId = null nghĩa là xem toàn chuỗi (Admin).
export async function listInventory({ branchId = null, keyword = '', stockStatus = '' } = {}) {
  const kw = keyword.trim().toLowerCase()
  const rows = await loadAllRows()
  return rows.filter((row) => {
    if (branchId && row.branchId !== branchId) return false
    if (stockStatus && row.stockStatus !== stockStatus) return false
    return matches(row, kw)
  })
}

// Các mã sắp hết và hết hàng, sắp theo mức thiếu lớn nhất.
export async function listLowStock({ branchId = null, keyword = '' } = {}) {
  const rows = await listInventory({ branchId, keyword })
  return rows
    .filter((row) => row.stockStatus !== STOCK_STATUS.AVAILABLE)
    .sort((a, b) => {
      if (a.stockStatus !== b.stockStatus) return a.stockStatus === STOCK_STATUS.OUT ? -1 : 1
      return b.minQuantity - b.quantity - (a.minQuantity - a.quantity)
    })
}

// Nhập thêm hàng cho một mã sản phẩm. Backend luôn nhập vào chi nhánh của tài khoản đang đăng nhập
// (Admin và Manager mới được nhập), nên tham số branchId chỉ để giữ chữ ký hàm cũ.
export async function adjustStock({ productId, quantity }) {
  if (!Number.isFinite(quantity) || quantity <= 0) {
    throw new Error('Số lượng nhập phải lớn hơn 0')
  }
  const saved = await client.post('/api/inventory/import', { productId, quantity })
  cache = null
  const products = await getProducts()
  return toRow(saved, products.find((p) => p.productId === productId))
}

// Việc trừ kho do backend tự làm khi thanh toán (POST /api/sales/checkout), nên không cần trừ ở frontend.
// Hàm này giữ lại tạm để màn thanh toán cũ chưa lỗi; cụm E sẽ xóa cùng lúc với việc sửa màn thanh toán.
export async function consumeStock() {
  cache = null
}

// Tổng quan tồn kho theo chi nhánh, dùng cho thẻ số liệu và tổng quan Admin.
export async function getInventorySummary({ branchId = null } = {}) {
  const rows = await listInventory({ branchId })
  const summary = {
    totalSkus: rows.length,
    totalQuantity: 0,
    available: 0,
    lowStock: 0,
    outOfStock: 0,
    totalValue: 0,
    branchBreakdown: [],
  }

  const byBranch = new Map()
  for (const row of rows) {
    summary.totalQuantity += row.quantity
    summary.totalValue += row.quantity * row.price
    if (row.stockStatus === STOCK_STATUS.OUT) summary.outOfStock += 1
    else if (row.stockStatus === STOCK_STATUS.LOW) summary.lowStock += 1
    else summary.available += 1

    const entry = byBranch.get(row.branchId) ?? {
      branchId: row.branchId,
      branchName: row.branchName,
      skus: 0,
      quantity: 0,
      value: 0,
      lowStock: 0,
      outOfStock: 0,
    }
    entry.skus += 1
    entry.quantity += row.quantity
    entry.value += row.quantity * row.price
    if (row.stockStatus === STOCK_STATUS.OUT) entry.outOfStock += 1
    else if (row.stockStatus === STOCK_STATUS.LOW) entry.lowStock += 1
    byBranch.set(row.branchId, entry)
  }

  summary.branchBreakdown = [...byBranch.values()].sort((a, b) => b.value - a.value)
  return summary
}