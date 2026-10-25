import { useEffect, useState } from 'react'
import { Activity, Clock, Database, Plug, RefreshCw, Server, Unplug, Wifi } from 'lucide-react'
import { Badge, Button, Card, DataTable, EmptyState, Spinner, StatCard } from '../../components/ui'
import { useAsyncData } from '../../hooks/useAsyncData'
import { formatDateTime } from '../../utils/format'
import {
  SITE_STATUS,
  SITE_STATUS_LABEL,
  getSites,
  getSyncLog,
  pingSite,
  syncSite,
  toggleSiteLink,
} from '../../services/siteService'

const TONE = { ONLINE: 'ok', DEGRADED: 'warn', OFFLINE: 'bad' }
const AUTO_REFRESH_MS = 10000

// "2 phút trước", "vừa xong"
function timeAgo(iso) {
  const sec = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000))
  if (sec < 60) return 'vừa xong'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min} phút trước`
  const hour = Math.floor(min / 60)
  return hour < 24 ? `${hour} giờ trước` : `${Math.floor(hour / 24)} ngày trước`
}

// Cách dữ liệu được chia giữa Central và các chi nhánh (theo thiết kế CSDL phân tán)
const FRAGMENTS = [
  { table: 'SanPham, KhachHang, ChiNhanh', type: 'Nhân bản toàn phần', flow: 'Central → mọi chi nhánh', note: 'Danh mục dùng chung, mọi site đều có bản đầy đủ' },
  { table: 'NhanVien, TonKho, HoaDon', type: 'Phân mảnh ngang theo MaChiNhanh', flow: 'Chi nhánh → Central', note: 'Mỗi chi nhánh chỉ giữ dữ liệu của mình' },
  { table: 'ChiTietHoaDon', type: 'Phân mảnh ngang dẫn xuất', flow: 'Chi nhánh → Central', note: 'Đi theo hóa đơn mà nó thuộc về' },
]

function SiteCard({ site, busy, notice, onPing, onSync, onToggle }) {
  const isCentral = site.kind === 'central'
  return (
    <div className="flex flex-col rounded-xl border border-ink/10 bg-card p-5">
      <div className="flex items-center gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent/15 text-accent">{isCentral ? <Database size={20} /> : <Server size={20} />}</span>
        <div className="min-w-0">
          <h3 className="font-display text-lg font-bold leading-tight text-ink">{site.name}</h3>
          <p className="truncate text-xs text-ink/50">{site.host}</p>
        </div>
      </div>

      <dl className="mt-4 space-y-2 text-sm">
        <div className="flex items-center justify-between gap-3">
          <dt className="text-ink/60">Trạng thái</dt>
          <dd>
            <Badge tone={TONE[site.status]}>{SITE_STATUS_LABEL[site.status]}</Badge>
          </dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-ink/60">Độ trễ</dt>
          <dd className="font-semibold tabular-nums text-ink">{site.status === SITE_STATUS.OFFLINE ? '—' : `${site.latencyMs} ms`}</dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-ink/60">{isCentral ? 'Tổng hợp gần nhất' : 'Đồng bộ gần nhất'}</dt>
          <dd className="font-semibold text-ink">{timeAgo(site.lastSyncAt)}</dd>
        </div>
        {!isCentral && (
          <div className="flex justify-between gap-3">
            <dt className="text-ink/60">Chờ đồng bộ</dt>
            <dd className="font-semibold tabular-nums text-ink">{site.pending} thay đổi</dd>
          </div>
        )}
      </dl>

      {notice && <p className={`mt-3 text-xs font-medium ${notice.tone === 'bad' ? 'text-bad' : 'text-ok'}`}>{notice.text}</p>}

      <div className="mt-auto flex flex-wrap gap-2 pt-4">
        <Button size="sm" variant="secondary" icon={Activity} loading={busy === 'ping'} disabled={!!busy && busy !== 'ping'} onClick={onPing}>
          Kiểm tra
        </Button>
        {!isCentral && (
          <>
            <Button size="sm" icon={RefreshCw} loading={busy === 'sync'} disabled={!!busy && busy !== 'sync'} onClick={onSync}>
              Đồng bộ ngay
            </Button>
            <Button
              size="sm"
              variant="ghost"
              icon={site.status === SITE_STATUS.OFFLINE ? Plug : Unplug}
              title={site.status === SITE_STATUS.OFFLINE ? 'Nối lại kết nối (giả lập)' : 'Ngắt kết nối (giả lập)'}
              aria-label={site.status === SITE_STATUS.OFFLINE ? 'Nối lại kết nối' : 'Ngắt kết nối thử'}
              disabled={!!busy}
              onClick={onToggle}
            />
          </>
        )}
      </div>
    </div>
  )
}

export default function SiteStatusPage() {
  const sitesQ = useAsyncData(getSites, [])
  const logQ = useAsyncData(() => getSyncLog({ limit: 30 }), [])
  const [busy, setBusy] = useState({}) // { siteId: 'ping' | 'sync' | 'toggle' }
  const [notice, setNotice] = useState({}) // { siteId: { tone, text } }

  const { reload: reloadSites } = sitesQ
  const { reload: reloadLog } = logQ
  const refreshAll = () => {
    reloadSites()
    reloadLog()
  }

  // Tự làm mới định kỳ để thấy số thay đổi chờ đồng bộ tăng khi một site mất kết nối
  useEffect(() => {
    const t = setInterval(() => {
      reloadSites()
      reloadLog()
    }, AUTO_REFRESH_MS)
    return () => clearInterval(t)
  }, [reloadSites, reloadLog])

  const run = async (siteId, kind, action) => {
    setBusy((b) => ({ ...b, [siteId]: kind }))
    setNotice((n) => ({ ...n, [siteId]: null }))
    try {
      const text = await action()
      if (text) setNotice((n) => ({ ...n, [siteId]: { tone: 'ok', text } }))
    } catch (err) {
      setNotice((n) => ({ ...n, [siteId]: { tone: 'bad', text: err.message } }))
    } finally {
      setBusy((b) => ({ ...b, [siteId]: null }))
      refreshAll()
    }
  }

  const sites = sitesQ.data ?? []
  const branches = sites.filter((s) => s.kind === 'branch')
  const online = sites.filter((s) => s.status !== SITE_STATUS.OFFLINE).length
  const pending = branches.reduce((sum, s) => sum + s.pending, 0)
  const alive = sites.filter((s) => s.status !== SITE_STATUS.OFFLINE)
  const avgLatency = alive.length ? Math.round(alive.reduce((sum, s) => sum + s.latencyMs, 0) / alive.length) : 0
  const lastSync = branches.length ? branches.reduce((a, b) => (new Date(a.lastSyncAt) > new Date(b.lastSyncAt) ? a : b)).lastSyncAt : null
  const siteName = (id) => sites.find((s) => s.siteId === id)?.name ?? id

  const logColumns = [
    { key: 'at', header: 'Thời gian', render: (r) => <span className="whitespace-nowrap">{formatDateTime(r.at)}</span> },
    { key: 'siteId', header: 'Site', render: (r) => <span className="whitespace-nowrap font-semibold">{siteName(r.siteId)}</span> },
    { key: 'mode', header: 'Kiểu' },
    { key: 'records', header: 'Bản ghi', align: 'right' },
    { key: 'durationMs', header: 'Thời gian chạy', align: 'right', render: (r) => `${(r.durationMs / 1000).toFixed(1)} giây` },
    {
      key: 'result',
      header: 'Kết quả',
      render: (r) => (
        <div>
          <Badge tone={r.result === 'OK' ? 'ok' : 'bad'}>{r.result === 'OK' ? 'Thành công' : 'Thất bại'}</Badge>
          {r.message && <p className="mt-1 text-xs text-ink/50">{r.message}</p>}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Trạng thái Site</h1>
          <p className="mt-1 text-sm text-ink/60">Tình trạng kết nối và đồng bộ giữa các chi nhánh với Central. Tự làm mới mỗi 10 giây.</p>
        </div>
        <Button variant="secondary" icon={RefreshCw} onClick={refreshAll}>
          Làm mới
        </Button>
      </header>

      {sitesQ.loading && !sitesQ.data ? (
        <Spinner />
      ) : sitesQ.error ? (
        <div className="rounded-xl border border-ink/10 bg-card">
          <EmptyState
            title="Không tải được trạng thái site"
            description={sitesQ.error}
            action={
              <Button variant="secondary" onClick={refreshAll}>
                Thử lại
              </Button>
            }
          />
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard icon={Wifi} tone={online === sites.length ? 'ok' : 'bad'} label="Site đang kết nối" value={`${online}/${sites.length}`} hint="Gồm Central và 3 chi nhánh" />
            <StatCard icon={Database} tone={pending > 40 ? 'warn' : 'accent'} label="Chờ đồng bộ" value={pending} hint="Thay đổi chưa về Central" />
            <StatCard icon={Clock} label="Đồng bộ gần nhất" value={<span className="whitespace-nowrap text-xl">{lastSync ? timeAgo(lastSync) : '—'}</span>} />
            <StatCard icon={Activity} label="Độ trễ trung bình" value={`${avgLatency} ms`} hint="Các site đang kết nối" />
          </div>

          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
            {sites.map((s) => (
              <SiteCard
                key={s.siteId}
                site={s}
                busy={busy[s.siteId]}
                notice={notice[s.siteId]}
                onPing={() =>
                  run(s.siteId, 'ping', async () => {
                    const r = await pingSite(s.siteId)
                    if (!r.ok) throw new Error('Không có phản hồi từ site')
                    return `Kết nối tốt, độ trễ ${r.latencyMs} ms`
                  })
                }
                onSync={() =>
                  run(s.siteId, 'sync', async () => {
                    const r = await syncSite(s.siteId)
                    return `Đã đồng bộ ${r.records} thay đổi về Central`
                  })
                }
                onToggle={() => run(s.siteId, 'toggle', () => toggleSiteLink(s.siteId).then(() => ''))}
              />
            ))}
          </div>
        </>
      )}

      <Card title="Cách dữ liệu được phân tán" padded={false}>
        <div className="mt-3 overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-ink/5 text-left text-[11px] font-bold uppercase tracking-wide text-ink/60">
                <th className="px-5 py-2.5">Bảng</th>
                <th className="px-5 py-2.5">Kiểu phân tán</th>
                <th className="px-5 py-2.5">Hướng đồng bộ</th>
                <th className="px-5 py-2.5">Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              {FRAGMENTS.map((f) => (
                <tr key={f.table} className="border-t border-ink/10">
                  <td className="px-5 py-3 font-semibold text-ink">{f.table}</td>
                  <td className="px-5 py-3">
                    <Badge tone="accent">{f.type}</Badge>
                  </td>
                  <td className="whitespace-nowrap px-5 py-3 text-ink/80">{f.flow}</td>
                  <td className="px-5 py-3 text-ink/60">{f.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <div>
        <h2 className="mb-3 text-sm font-bold text-ink">Nhật ký đồng bộ</h2>
        {logQ.loading && !logQ.data ? (
          <Spinner />
        ) : (
          <DataTable columns={logColumns} rows={logQ.data ?? []} pageSize={6} emptyText="Chưa có lần đồng bộ nào" />
        )}
      </div>
    </div>
  )
}