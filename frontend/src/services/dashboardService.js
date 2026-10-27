import { buildQuery, client } from '../api'

// Tổng quan lấy từ backend (/api/dashboard); backend tổng hợp sẵn doanh thu, tồn kho, top sản phẩm...
// Các trang cần thêm: key cho dòng tồn kho, rank cho nhân viên (backend không trả hai trường này).

const withLowKey = (items = []) => items.map((i) => ({ ...i, key: `${i.branchId}-${i.productId}` }))
const withRank = (items = []) => items.map((e, index) => ({ ...e, rank: e.rank ?? index + 1 }))

// Tổng quan cho trang Tổng quan của Admin: toàn chuỗi, N ngày gần nhất.
export async function getAdminDashboard({ days = 30 } = {}) {
  const res = await client.get(`/api/dashboard/admin${buildQuery({ days })}`)
  return { ...res, lowStockItems: withLowKey(res.lowStockItems), employeeSales: withRank(res.employeeSales) }
}

// Tổng quan cho Manager (chi nhánh trong token). Trả về đúng dạng trang Manager đang dùng.
export async function getManagerDashboard({ days = 30 } = {}) {
  const res = await client.get(`/api/dashboard/manager${buildQuery({ days })}`)
  return {
    summary: res.summary,
    series: res.revenueSeries,
    top: res.topProducts,
    employees: withRank(res.employeeSales),
    inventory: res.inventory,
    low: withLowKey(res.lowStockItems),
  }
}