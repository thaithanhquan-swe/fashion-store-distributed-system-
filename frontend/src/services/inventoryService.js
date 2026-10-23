import { getBranch } from '../config/branches'
import { STOCK_STATUS } from '../utils/format'
import { INVENTORY, PRODUCTS, wait } from './mockData'

// Backend chưa có API tồn kho, nên các hàm dưới đây trả dữ liệu giả.
// Khi có API, chỉ thay phần thân hàm, trang giao diện giữ nguyên.

const MOCK_DELAY = 250
const PRODUCT_MAP = new Map(PRODUCTS.map((p) => [p.productId, p]))

// Sắp hết khi số lượng tồn <= mức tồn tối thiểu, hết hàng khi không còn sản phẩm.
const stockStatusOf = (quantity, minQuantity) => {
  if (quantity <= 0) return STOCK_STATUS.OUT
  if (quantity <= minQuantity) return STOCK_STATUS.LOW
  return STOCK_STATUS.AVAILABLE
}

// Tồn kho giả được giữ trong bộ nhớ để thao tác nhập kho có hiệu lực trong phiên làm việc.
let stockRows = INVENTORY.map((row) => ({ ...row }))

const toRow = (row) => {
  const product = PRODUCT_MAP.get(row.productId)
  return {
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
    minQuantity: row.minQuantity,
    updatedAt: row.updatedAt,
    stockStatus: stockStatusOf(row.quantity, row.minQuantity),
  }
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
  await wait(MOCK_DELAY)
  const kw = keyword.trim().toLowerCase()
  return stockRows.map(toRow).filter((row) => {
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

// Nhập thêm hàng cho một mã sản phẩm tại chi nhánh.
export async function adjustStock({ branchId, productId, quantity }) {
  await wait(MOCK_DELAY)
  const row = stockRows.find((item) => item.branchId === branchId && item.productId === productId)
  if (!row) {
    throw new Error('Không tìm thấy mã sản phẩm trong tồn kho của chi nhánh')
  }
  if (!Number.isFinite(quantity) || quantity <= 0) {
    throw new Error('Số lượng nhập phải lớn hơn 0')
  }
  row.quantity += quantity
  row.updatedAt = new Date().toISOString()
  return toRow(row)
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
