import { BRANCHES } from '../config/branches'
import { getEmployees } from './employeeService'
import { getInventorySummary, listLowStock } from './inventoryService'
import { getEmployeeSales, getRevenueSeries, getRevenueSummary } from './statisticsService'

// Dữ liệu chi nhánh. Backend chưa có API chi nhánh nên địa chỉ, số điện thoại là dữ liệu giả,
// còn doanh thu, tồn kho, nhân viên được ghép từ các service giả khác.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
// Bám theo bảng ChiNhanh: branchId (MaChiNhanh), name (TenChiNhanh), address (DiaChi), phone (SDT).

const DAYS = 30

const INFO = {
  CN01: { address: '125 Nguyễn Huệ, P. Bến Nghé, Quận 1, TP.HCM', phone: '028 3822 1001', openedAt: '2021-03-15' },
  CN02: { address: '58 Nguyễn Thị Thập, P. Tân Phong, Quận 7, TP.HCM', phone: '028 3775 2002', openedAt: '2022-08-20' },
  CN03: { address: '210 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP.HCM', phone: '028 3896 3003', openedAt: '2023-05-10' },
}

// Gộp thông tin cơ bản và số liệu 30 ngày gần nhất của một chi nhánh.
async function buildOverview(branch) {
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
    ...INFO[branch.id],
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
  return Promise.all(BRANCHES.map(buildOverview))
}

// Chi tiết một chi nhánh: tổng quan, doanh thu 14 ngày, nhân viên, hàng cần chú ý, doanh số nhân viên.
export async function getBranchDetail(branchId) {
  const branch = BRANCHES.find((b) => b.id === branchId)
  if (!branch) throw new Error('Không tìm thấy chi nhánh')

  const [overview, series, employees, lowStock, sales] = await Promise.all([
    buildOverview(branch),
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