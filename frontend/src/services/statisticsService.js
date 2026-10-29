import { BRANCHES } from '../config/branches'
import { buildQuery, client } from '../api'

// Thống kê lấy từ backend (/api/statistics). Admin xem toàn chuỗi hoặc từng chi nhánh;
// Manager chỉ xem chi nhánh mình (backend tự lấy từ token); Staff chỉ gọi được top-products.
// Số liệu do backend tổng hợp theo N ngày gần nhất (days).

// Màn hình thường gọi doanh thu và chuỗi theo ngày cùng lúc; giữ kết quả 2 giây để chỉ gọi API một lần.
const CACHE_MS = 2000
const cache = new Map()

function fetchRevenue(branchId, days) {
  const key = `${branchId ?? ''}|${days}`
  const hit = cache.get(key)
  if (hit && Date.now() - hit.at < CACHE_MS) return hit.promise
  const promise = client.get(`/api/statistics/revenue${buildQuery({ branchId, days })}`)
  cache.set(key, { at: Date.now(), promise })
  promise.catch(() => cache.delete(key))
  return promise
}

// Tổng quan doanh thu trong khoảng thời gian, kèm mức tăng giảm so với kỳ trước.
export async function getRevenueSummary({ branchId = null, days = 30 } = {}) {
  return (await fetchRevenue(branchId, days)).summary
}

// Doanh thu theo từng ngày trong khoảng thời gian.
export async function getRevenueSeries({ branchId = null, days = 30 } = {}) {
  return (await fetchRevenue(branchId, days)).series
}

// Doanh thu và số hóa đơn của từng chi nhánh trong khoảng thời gian.
export async function getRevenueByBranch({ days = 30 } = {}) {
  return client.get(`/api/statistics/revenue/branches${buildQuery({ days })}`)
}

// Sản phẩm bán chạy. sortBy: 'revenue' hoặc 'quantity'.
export async function getTopProducts({ branchId = null, days = 30, limit = 10, sortBy = 'revenue' } = {}) {
  return client.get(`/api/statistics/top-products${buildQuery({ branchId, days, limit, sortBy })}`)
}

// Doanh thu từng ngày theo chi nhánh, dùng để so sánh giữa các chi nhánh.
// Backend chưa có endpoint ma trận, nên gọi doanh thu của từng chi nhánh rồi ghép theo ngày.
export async function getRevenueMatrix({ days = 14 } = {}) {
  const settled = await Promise.allSettled(BRANCHES.map((b) => fetchRevenue(b.id, days)))
  const results = settled.map((r) => (r.status === 'fulfilled' ? r.value : { series: [] }))
  const firstSeries = results.find((r) => Array.isArray(r.series) && r.series.length > 0)?.series || []
  if (firstSeries.length === 0) return []
  return firstSeries.map((day, index) => {
    const row = { date: day.date, dateLabel: day.dateLabel, total: 0 }
    BRANCHES.forEach((b, i) => {
      const value = Number(results[i]?.series?.[index]?.revenue ?? 0)
      row[b.id] = value
      row.total += value
    })
    return row
  })
}

// Doanh số bán hàng theo nhân viên (Admin, Manager).
export async function getEmployeeSales({ branchId = null, days = 30 } = {}) {
  return client.get(`/api/statistics/employees${buildQuery({ branchId, days, limit: 50 })}`)
}