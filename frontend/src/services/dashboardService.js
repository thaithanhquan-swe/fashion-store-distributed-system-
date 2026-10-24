import { getInventorySummary, listLowStock } from './inventoryService'
import {
  getEmployeeSales,
  getRevenueByBranch,
  getRevenueSeries,
  getRevenueSummary,
  getTopProducts,
} from './statisticsService'

// Backend chưa có API tổng quan, nên hàm dưới đây ghép từ các API giả khác.
// Khi có API, chỉ thay phần thân hàm, trang giao diện giữ nguyên.

// Dữ liệu tổng quan cho trang Tổng quan của Admin: toàn chuỗi, 30 ngày gần nhất.
export async function getAdminDashboard({ days = 30 } = {}) {
  const [revenue, inventory, series, branches, topProducts, lowStock, employees] = await Promise.all([
    getRevenueSummary({ branchId: null, days }),
    getInventorySummary({ branchId: null }),
    getRevenueSeries({ branchId: null, days: Math.min(days, 14) }),
    getRevenueByBranch({ days }),
    getTopProducts({ branchId: null, days, limit: 5 }),
    listLowStock({ branchId: null }),
    getEmployeeSales({ branchId: null, days }),
  ])

  return {
    summary: revenue,
    inventory,
    revenueSeries: series,
    branchRevenue: branches,
    topProducts,
    lowStockItems: lowStock.slice(0, 8),
    lowStockTotal: lowStock.length,
    employeeSales: employees.slice(0, 5),
  }
}
