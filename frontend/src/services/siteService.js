import { client, buildQuery } from '../api'

// Trạng thái các site của hệ thống phân tán: Central và ba chi nhánh.
// Dữ liệu được gọi trực tiếp từ backend (/api/sites).
//
// Mỗi chi nhánh ghi dữ liệu tại site của mình (hóa đơn, tồn kho, nhân viên) rồi đồng bộ về Central.
// Central nhân bản danh mục dùng chung (sản phẩm, khách hàng, chi nhánh) xuống các site.

export const SITE_STATUS = { ONLINE: 'ONLINE', DEGRADED: 'DEGRADED', OFFLINE: 'OFFLINE' }
export const SITE_STATUS_LABEL = { ONLINE: 'Hoạt động', DEGRADED: 'Chậm', OFFLINE: 'Mất kết nối' }

// Danh sách site kèm trạng thái và số thay đổi đang chờ đồng bộ.
export async function getSites() {
  return client.get('/api/sites')
}

// Nhật ký đồng bộ gần đây, mới nhất trước.
export async function getSyncLog({ limit = 30 } = {}) {
  return client.get(`/api/sites/sync-log${buildQuery({ limit })}`)
}

// Tóm tắt cho thanh trên cùng. Truyền branchId để chỉ xét Central và site của chi nhánh đó.
export async function getSiteSummary({ branchId = null } = {}) {
  return client.get(`/api/sites/summary${buildQuery({ branchId })}`)
}

// Kiểm tra kết nối tới một site, trả về độ trễ.
export async function pingSite(siteId) {
  return client.post(`/api/sites/${siteId}/ping`)
}

// Kích hoạt đồng bộ thủ công giữa site và Central.
export async function syncSite(siteId) {
  return client.post(`/api/sites/${siteId}/sync`)
}

// Bật/tắt kết nối giả lập để thử nghiệm sự cố mất kết nối mạng (phục vụ demo).
export async function toggleSiteLink(siteId) {
  return client.post(`/api/sites/${siteId}/toggle`)
}
