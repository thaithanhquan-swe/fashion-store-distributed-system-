import { BRANCHES, getBranch } from '../config/branches'
import { client } from '../api'
import { getEmployees } from './employeeService'
import { getInventorySummary, listLowStock } from './inventoryService'
import { getEmployeeSales, getRevenueSeries, getRevenueSummary } from './statisticsService'

// Dữ liệu chi nhánh: địa chỉ, số điện thoại lấy từ backend (/api/branches);
// doanh thu, tồn kho, nhân viên được ghép từ các service khác.

const DAYS = 30

// Gộp thông tin cơ bản và số liệu 30 ngày gần nhất của một chi nhánh.
// info: dữ liệu chi nhánh từ backend ({ address, phoneNumber, ... }).
async function buildOverview(branch, info) {
  const [employees, revenue, inventory] = await Promise.all([
    getEmployees({ branchId: branch.id }).catch(() => []),
    getRevenueSummary({ branchId: branch.id, days: DAYS }).catch(() => ({ revenue: 0, revenueChangePercent: 0, orders: 0 })),
    getInventorySummary({ branchId: branch.id }).catch(() => ({ lowStock: 0, outOfStock: 0, totalValue: 0 })),
  ])
  const working = (employees ?? []).filter((e) => e.status === 'WORKING')
  return {
    branchId: branch.id,
    name: branch.name,
    short: branch.short,
    address: info?.address ?? '',
    phone: info?.phoneNumber ?? '',
    manager: working.find((e) => e.position === 'Quản lý chi nhánh')?.employeeName ?? 'Chưa có quản lý',
    employeeCount: working.length,
    revenue: revenue.revenue ?? 0,
    revenueChangePercent: revenue.revenueChangePercent ?? 0,
    orders: revenue.orders ?? 0,
    lowStock: inventory.lowStock ?? 0,
    outOfStock: inventory.outOfStock ?? 0,
    stockValue: inventory.totalValue ?? 0,
  }
}

// Danh sách 3 chi nhánh cố định của hệ thống kèm số liệu tổng quan.
export async function getBranchList() {
  const infos = await client.get('/api/branches').catch(() => [])
  return Promise.all(
    BRANCHES.map((b) => {
      const info = (infos ?? []).find(
        (i) =>
          i.branchId?.toLowerCase() === b.id.toLowerCase() ||
          i.branchId?.replace(/^CN0+/, 'CN').toLowerCase() === b.id.replace(/^CN0+/, 'CN').toLowerCase()
      )
      return buildOverview(b, info)
    })
  )
}

// Chi tiết một chi nhánh: tổng quan, doanh thu 14 ngày, nhân viên, hàng cần chú ý, doanh số nhân viên.
export async function getBranchDetail(branchId) {
  const info = await client.get(`/api/branches/${branchId}`).catch(() => null)
  const meta = getBranch(branchId)
  const branch = {
    id: branchId,
    name: info?.branchName || meta.name,
    short: meta.short || info?.branchName || branchId,
  }

  const [overview, series, employees, lowStock, sales] = await Promise.all([
    buildOverview(branch, info),
    getRevenueSeries({ branchId, days: 14 }).catch(() => []),
    getEmployees({ branchId }).catch(() => []),
    listLowStock({ branchId }).catch(() => []),
    getEmployeeSales({ branchId, days: DAYS }).catch(() => []),
  ])

  return {
    ...overview,
    revenueSeries: series ?? [],
    employees: (employees ?? []).filter((e) => e.status === 'WORKING'),
    lowStockItems: (lowStock ?? []).slice(0, 6),
    topSellers: (sales ?? []).slice(0, 3),
  }
}