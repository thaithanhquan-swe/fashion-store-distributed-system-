import { BRANCHES } from '../config/branches'
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
    getEmployees({ branchId: branch.id }),
    getRevenueSummary({ branchId: branch.id, days: DAYS }),
    getInventorySummary({ branchId: branch.id }),
  ])
  const working = employees.filter((e) => e.status === 'WORKING')
  return {
    branchId: branch.id,
    name: branch.name,
    short: branch.short,
    address: info?.address ?? '',
    phone: info?.phoneNumber ?? '',
    manager: working.find((e) => e.position === 'Quản lý chi nhánh')?.employeeName ?? 'Chưa có quản lý',
    employeeCount: working.length,
    revenue: revenue.revenue,
    revenueChangePercent: revenue.revenueChangePercent,
    orders: revenue.orders,
    lowStock: inventory.lowStock,
    outOfStock: inventory.outOfStock,
    stockValue: inventory.totalValue,
  }
}

// Danh sách ba chi nhánh kèm số liệu tổng quan.
export async function getBranchList() {
  const infos = await client.get('/api/branches')
  return Promise.all(BRANCHES.map((b) => buildOverview(b, infos.find((i) => i.branchId === b.id))))
}

// Chi tiết một chi nhánh: tổng quan, doanh thu 14 ngày, nhân viên, hàng cần chú ý, doanh số nhân viên.
export async function getBranchDetail(branchId) {
  const branch = BRANCHES.find((b) => b.id === branchId)
  if (!branch) throw new Error('Không tìm thấy chi nhánh')

  const info = await client.get(`/api/branches/${branchId}`)
  const [overview, series, employees, lowStock, sales] = await Promise.all([
    buildOverview(branch, info),
    getRevenueSeries({ branchId, days: 14 }),
    getEmployees({ branchId }),
    listLowStock({ branchId }),
    getEmployeeSales({ branchId, days: DAYS }),
  ])

  return {
    ...overview,
    revenueSeries: series,
    employees: employees.filter((e) => e.status === 'WORKING'),
    lowStockItems: lowStock.slice(0, 6),
    topSellers: sales.slice(0, 3),
  }
}