import { getBranch } from '../config/branches'
import { BRANCH_IDS, EMPLOYEES, wait } from './mockData'
import { getAllInvoiceDetails, getAllInvoices } from './invoiceService'

// Backend chưa có API thống kê, nên các hàm dưới đây trả dữ liệu giả.
// Khi có API, chỉ thay phần thân hàm, trang giao diện giữ nguyên.

const MOCK_DELAY = 250
const EMPLOYEE_MAP = new Map(EMPLOYEES.map((e) => [e.employeeId, e]))

const startOfDay = (date) => {
  const value = new Date(date)
  value.setHours(0, 0, 0, 0)
  return value
}

// Khoảng thời gian hiện tại: N ngày gần nhất tính từ đầu ngày hôm nay.
const currentWindow = (days) => {
  const to = new Date()
  const from = startOfDay(to)
  from.setDate(from.getDate() - (days - 1))
  return { from, to }
}

// Khoảng liền trước, dùng để so sánh tăng giảm.
const previousWindow = (days) => {
  const current = currentWindow(days)
  const to = current.from
  const from = startOfDay(to)
  from.setDate(from.getDate() - days)
  return { from, to }
}

const inWindow = (value, window) => {
  const time = new Date(value).getTime()
  return time >= window.from.getTime() && time <= window.to.getTime()
}

// Khoá ngày theo giờ địa phương, tránh lệch một ngày khi chuyển sang UTC.
const dayKey = (date) => {
  const value = new Date(date)
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${value.getFullYear()}-${month}-${day}`
}

const filterInvoices = (branchId, days, window) =>
  getAllInvoices().filter(
    (invoice) => (!branchId || invoice.branchId === branchId) && inWindow(invoice.createdAt, window),
  )

const percentChange = (current, previous) => {
  if (!previous) return current ? 100 : 0
  return Math.round(((current - previous) / previous) * 1000) / 10
}

// Tổng quan doanh thu trong khoảng thời gian, kèm mức tăng giảm so với kỳ trước.
export async function getRevenueSummary({ branchId = null, days = 30 } = {}) {
  await wait(MOCK_DELAY)
  const current = filterInvoices(branchId, days, currentWindow(days))
  const previous = filterInvoices(branchId, days, previousWindow(days))

  const revenue = current.reduce((sum, invoice) => sum + invoice.totalAmount, 0)
  const previousRevenue = previous.reduce((sum, invoice) => sum + invoice.totalAmount, 0)
  const customers = new Set(current.map((invoice) => invoice.customerId)).size
  const previousCustomers = new Set(previous.map((invoice) => invoice.customerId)).size

  return {
    revenue,
    orders: current.length,
    customers,
    averageOrderValue: current.length ? Math.round(revenue / current.length) : 0,
    averageDailyRevenue: current.length ? Math.round(revenue / days) : 0,
    previousRevenue,
    previousOrders: previous.length,
    revenueChangePercent: percentChange(revenue, previousRevenue),
    ordersChangePercent: percentChange(current.length, previous.length),
    customersChangePercent: percentChange(customers, previousCustomers),
  }
}

// Doanh thu theo từng ngày trong khoảng thời gian.
export async function getRevenueSeries({ branchId = null, days = 30 } = {}) {
  await wait(MOCK_DELAY)
  const window = currentWindow(days)
  const invoices = filterInvoices(branchId, days, window)
  const series = []

  for (let index = days - 1; index >= 0; index--) {
    const date = new Date(window.to)
    date.setDate(date.getDate() - index)
    date.setHours(0, 0, 0, 0)
    const key = dayKey(date)
    const dayInvoices = invoices.filter((invoice) => dayKey(invoice.createdAt) === key)
    series.push({
      date: key,
      dateLabel: date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      revenue: dayInvoices.reduce((sum, invoice) => sum + invoice.totalAmount, 0),
      orders: dayInvoices.length,
    })
  }
  return series
}

// Doanh thu và số hóa đơn của từng chi nhánh trong khoảng thời gian.
export async function getRevenueByBranch({ days = 30 } = {}) {
  await wait(MOCK_DELAY)
  const window = currentWindow(days)
  const invoices = filterInvoices(null, days, window)
  const total = invoices.reduce((sum, invoice) => sum + invoice.totalAmount, 0)

  const byBranch = new Map()
  for (const invoice of invoices) {
    const entry = byBranch.get(invoice.branchId) ?? {
      branchId: invoice.branchId,
      branchName: getBranch(invoice.branchId)?.name ?? invoice.branchId,
      revenue: 0,
      orders: 0,
      items: 0,
    }
    entry.revenue += invoice.totalAmount
    entry.orders += 1
    byBranch.set(invoice.branchId, entry)
  }

  return [...byBranch.values()]
    .map((entry) => ({
      ...entry,
      share: total ? Math.round((entry.revenue / total) * 1000) / 10 : 0,
      averageOrderValue: entry.orders ? Math.round(entry.revenue / entry.orders) : 0,
    }))
    .sort((a, b) => b.revenue - a.revenue)
}

// Sản phẩm bán chạy: gom từ ChiTietHoaDon trong khoảng thời gian.
// sortBy: 'revenue' hoặc 'quantity'.
export async function getTopProducts({ branchId = null, days = 30, limit = 10, sortBy = 'revenue' } = {}) {
  await wait(MOCK_DELAY)
  const window = currentWindow(days)
  const invoices = filterInvoices(branchId, days, window)
  const invoiceIds = new Set(invoices.map((invoice) => invoice.invoiceId))
  const lines = getAllInvoiceDetails().filter((line) => invoiceIds.has(line.invoiceId))

  const byProduct = new Map()
  for (const line of lines) {
    const entry = byProduct.get(line.productId) ?? {
      productId: line.productId,
      productName: line.productName,
      quantity: 0,
      revenue: 0,
      invoices: new Set(),
    }
    entry.quantity += line.quantity
    entry.revenue += line.lineTotal
    entry.invoices.add(line.invoiceId)
    byProduct.set(line.productId, entry)
  }

  return [...byProduct.values()]
    .map((entry) => ({
      productId: entry.productId,
      productName: entry.productName,
      quantity: entry.quantity,
      revenue: entry.revenue,
      invoices: entry.invoices.size,
    }))
    .sort((a, b) => (sortBy === 'quantity' ? b.quantity - a.quantity : b.revenue - a.revenue))
    .slice(0, limit)
    .map((entry, index) => ({ ...entry, rank: index + 1 }))
}

// Doanh thu từng ngày theo chi nhánh, dùng để so sánh giữa các chi nhánh.
export async function getRevenueMatrix({ days = 14 } = {}) {
  await wait(MOCK_DELAY)
  const window = currentWindow(days)
  const invoices = filterInvoices(null, days, window)
  const series = []

  for (let index = days - 1; index >= 0; index--) {
    const date = new Date(window.to)
    date.setDate(date.getDate() - index)
    date.setHours(0, 0, 0, 0)
    const key = dayKey(date)
    const dayInvoices = invoices.filter((invoice) => dayKey(invoice.createdAt) === key)
    const row = {
      date: key,
      dateLabel: date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      total: 0,
    }
    for (const branchId of BRANCH_IDS) {
      const value = dayInvoices
        .filter((invoice) => invoice.branchId === branchId)
        .reduce((sum, invoice) => sum + invoice.totalAmount, 0)
      row[branchId] = value
      row.total += value
    }
    series.push(row)
  }
  return series
}

// Doanh số bán hàng theo nhân viên.
export async function getEmployeeSales({ branchId = null, days = 30 } = {}) {
  await wait(MOCK_DELAY)
  const window = currentWindow(days)
  const invoices = filterInvoices(branchId, days, window)
  const invoiceIds = new Set(invoices.map((invoice) => invoice.invoiceId))
  const lines = getAllInvoiceDetails().filter((line) => invoiceIds.has(line.invoiceId))

  const byEmployee = new Map()
  for (const invoice of invoices) {
    const employee = EMPLOYEE_MAP.get(invoice.employeeId)
    const entry = byEmployee.get(invoice.employeeId) ?? {
      employeeId: invoice.employeeId,
      employeeName: employee?.employeeName ?? invoice.employeeId,
      branchId: invoice.branchId,
      branchName: getBranch(invoice.branchId)?.name ?? invoice.branchId,
      position: employee?.position ?? '',
      orders: 0,
      revenue: 0,
      items: 0,
    }
    entry.orders += 1
    entry.revenue += invoice.totalAmount
    byEmployee.set(invoice.employeeId, entry)
  }

  const invoiceMap = new Map(invoices.map((invoice) => [invoice.invoiceId, invoice]))
  for (const line of lines) {
    const invoice = invoiceMap.get(line.invoiceId)
    const entry = invoice ? byEmployee.get(invoice.employeeId) : null
    if (entry) entry.items += line.quantity
  }

  return [...byEmployee.values()]
    .sort((a, b) => b.revenue - a.revenue)
    .map((entry, index) => ({ ...entry, rank: index + 1 }))
}
