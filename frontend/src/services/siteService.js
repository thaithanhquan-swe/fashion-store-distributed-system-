import { wait } from './mockData'
import { loadMock, saveMock } from './mockStore'

// Trạng thái các site của hệ thống phân tán: Central và ba chi nhánh.
// Backend chưa có API giám sát, nên dữ liệu ở đây là giả lập.
// Khi có API: thay thân các hàm bằng fetch(`${API_URL}/...`), giữ nguyên dữ liệu trả về.
//
// Mỗi chi nhánh ghi dữ liệu tại site của mình (hóa đơn, tồn kho, nhân viên) rồi đồng bộ về Central.
// Central nhân bản danh mục dùng chung (sản phẩm, khách hàng, chi nhánh) xuống các site.

export const SITE_STATUS = { ONLINE: 'ONLINE', DEGRADED: 'DEGRADED', OFFLINE: 'OFFLINE' }
export const SITE_STATUS_LABEL = { ONLINE: 'Hoạt động', DEGRADED: 'Chậm', OFFLINE: 'Mất kết nối' }

const SLOW_MS = 300 // độ trễ từ mức này trở lên coi là chậm
const PENDING_STEP_MS = 5000 // khi site mất kết nối, cứ 5 giây có thêm 1 thay đổi chờ đồng bộ

const minutesAgo = (m) => new Date(Date.now() - m * 60000).toISOString()

// link: 'UP' | 'DOWN'. pendingBase: số thay đổi đang chờ. downSince: lúc bắt đầu mất kết nối.
const SEED_SITES = [
  { siteId: 'CENTRAL', name: 'Central', kind: 'central', host: 'central.fashionstore.local', baseLatency: 18, latencyMs: 18, link: 'UP', lastSyncAt: minutesAgo(1), pendingBase: 0, downSince: null },
  { siteId: 'CN01', name: 'Site Quận 1', kind: 'branch', branchId: 'CN001', host: 'q1.fashionstore.local', baseLatency: 42, latencyMs: 42, link: 'UP', lastSyncAt: minutesAgo(2), pendingBase: 3, downSince: null },
  { siteId: 'CN02', name: 'Site Quận 3', kind: 'branch', branchId: 'CN002', host: 'q3.fashionstore.local', baseLatency: 65, latencyMs: 65, link: 'UP', lastSyncAt: minutesAgo(4), pendingBase: 7, downSince: null },
  { siteId: 'CN03', name: 'Site Thủ Đức', kind: 'branch', branchId: 'CN003', host: 'td.fashionstore.local', baseLatency: 410, latencyMs: 410, link: 'UP', lastSyncAt: minutesAgo(38), pendingBase: 41, downSince: null },
]

const SEED_LOG = [
  { id: 'L008', at: minutesAgo(2), siteId: 'CN01', mode: 'Tự động', records: 6, durationMs: 480, result: 'OK', message: '' },
  { id: 'L007', at: minutesAgo(4), siteId: 'CN02', mode: 'Tự động', records: 11, durationMs: 730, result: 'OK', message: '' },
  { id: 'L006', at: minutesAgo(9), siteId: 'CN01', mode: 'Tự động', records: 4, durationMs: 410, result: 'OK', message: '' },
  { id: 'L005', at: minutesAgo(14), siteId: 'CN02', mode: 'Tự động', records: 9, durationMs: 690, result: 'OK', message: '' },
  { id: 'L004', at: minutesAgo(38), siteId: 'CN03', mode: 'Tự động', records: 0, durationMs: 5000, result: 'FAILED', message: 'Hết thời gian chờ phản hồi' },
  { id: 'L003', at: minutesAgo(43), siteId: 'CN03', mode: 'Tự động', records: 14, durationMs: 2100, result: 'OK', message: '' },
  { id: 'L002', at: minutesAgo(52), siteId: 'CN01', mode: 'Tự động', records: 5, durationMs: 450, result: 'OK', message: '' },
  { id: 'L001', at: minutesAgo(58), siteId: 'CN02', mode: 'Thủ công', records: 17, durationMs: 880, result: 'OK', message: '' },
]

let sites = loadMock('sites', SEED_SITES)
let log = loadMock('sitelog', SEED_LOG)
const commit = (nextSites, nextLog = log) => {
  sites = nextSites
  log = nextLog
  saveMock('sites', sites)
  saveMock('sitelog', log)
}

const statusOf = (s) => {
  if (s.link === 'DOWN') return SITE_STATUS.OFFLINE
  return s.latencyMs >= SLOW_MS ? SITE_STATUS.DEGRADED : SITE_STATUS.ONLINE
}

const derive = (s) => ({
  ...s,
  status: statusOf(s),
  pending: s.kind === 'central' ? 0 : s.pendingBase + (s.downSince ? Math.floor((Date.now() - new Date(s.downSince).getTime()) / PENDING_STEP_MS) : 0),
})

const find = (siteId) => {
  const s = sites.find((x) => x.siteId === siteId)
  if (!s) throw new Error('Không tìm thấy site')
  return s
}

// Danh sách site kèm trạng thái và số thay đổi đang chờ đồng bộ.
export async function getSites() {
  await wait(250)
  return sites.map(derive)
}

// Nhật ký đồng bộ gần đây, mới nhất trước.
export async function getSyncLog({ limit = 30 } = {}) {
  await wait(200)
  return [...log].sort((a, b) => new Date(b.at) - new Date(a.at)).slice(0, limit)
}

// Tóm tắt cho thanh trên cùng. Truyền branchId để chỉ xét Central và site của chi nhánh đó.
export async function getSiteSummary({ branchId = null } = {}) {
  await wait(100)
  const normId = (id) => (id ? id.replace(/^CN0+/, 'CN').toUpperCase() : '')
  const relevant = sites.filter((s) => !branchId || s.kind === 'central' || normId(s.branchId) === normId(branchId)).map(derive)
  const offline = relevant.filter((s) => s.status === SITE_STATUS.OFFLINE).length
  const degraded = relevant.filter((s) => s.status === SITE_STATUS.DEGRADED).length
  return {
    status: offline ? SITE_STATUS.OFFLINE : degraded ? SITE_STATUS.DEGRADED : SITE_STATUS.ONLINE,
    offline,
    degraded,
    pending: relevant.reduce((sum, s) => sum + s.pending, 0),
  }
}

// Kiểm tra kết nối tới một site, trả về độ trễ.
export async function pingSite(siteId) {
  await wait(600)
  const s = find(siteId)
  if (s.link === 'DOWN') return { ok: false, latencyMs: null }
  const latencyMs = Math.round(s.baseLatency * (0.85 + Math.random() * 0.3))
  commit(sites.map((x) => (x.siteId === siteId ? { ...x, latencyMs } : x)))
  return { ok: true, latencyMs }
}

// Đồng bộ ngay một chi nhánh về Central. Ném Error nếu site không phản hồi.
export async function syncSite(siteId) {
  await wait(1200)
  const s = find(siteId)
  if (s.kind === 'central') throw new Error('Central là nơi nhận dữ liệu, không cần đồng bộ')
  const now = new Date().toISOString()
  const entry = { id: `L${Date.now()}`, at: now, siteId, mode: 'Thủ công' }

  if (s.link === 'DOWN') {
    commit(sites, [{ ...entry, records: 0, durationMs: 5000, result: 'FAILED', message: 'Site không phản hồi' }, ...log])
    throw new Error('Site không phản hồi, dữ liệu vẫn được giữ lại và sẽ đồng bộ khi có kết nối')
  }

  const records = derive(s).pending
  commit(
    sites.map((x) => (x.siteId === siteId ? { ...x, lastSyncAt: now, pendingBase: 0, downSince: null } : x)),
    [{ ...entry, records, durationMs: 400 + records * 25, result: 'OK', message: '' }, ...log],
  )
  return { records }
}

// Giả lập ngắt hoặc nối lại kết nối của một chi nhánh (dùng để minh họa khi demo).
export async function toggleSiteLink(siteId) {
  await wait(300)
  const s = find(siteId)
  if (s.kind === 'central') throw new Error('Không giả lập ngắt Central')
  if (s.link === 'UP') {
    commit(sites.map((x) => (x.siteId === siteId ? { ...x, link: 'DOWN', downSince: new Date().toISOString() } : x)))
  } else {
    const pending = derive(s).pending
    commit(sites.map((x) => (x.siteId === siteId ? { ...x, link: 'UP', pendingBase: pending, downSince: null } : x)))
  }
}