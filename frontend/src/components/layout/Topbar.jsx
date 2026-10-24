import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CircleAlert, LogOut, Menu, RefreshCw, WifiOff } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useAsyncData } from '../../hooks/useAsyncData'
import { getSiteSummary } from '../../services/siteService'
import { getBranch } from '../../config/branches'
import { ROLE_LABEL } from '../../config/nav'

// Nhãn trạng thái đồng bộ theo tình trạng các site
const SYNC_BADGE = {
  ONLINE: { cls: 'bg-ok/15 text-ok', icon: RefreshCw, text: 'Đã đồng bộ Central' },
  DEGRADED: { cls: 'bg-warn/15 text-warn', icon: CircleAlert, text: 'Đồng bộ chậm' },
  OFFLINE: { cls: 'bg-bad/15 text-bad', icon: WifiOff, text: 'Mất kết nối Central' },
}

// Thanh trên cùng: chi nhánh đang làm việc, trạng thái đồng bộ Central, tài khoản.
export default function Topbar({ onMenu }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const branch = getBranch(user.branchId)
  const initial = user.name.trim().split(' ').pop()[0]

  // Trạng thái đồng bộ: Admin xem toàn chuỗi, chi nhánh chỉ xét site của mình và Central. Tự làm mới 15 giây.
  const { data: sync, reload } = useAsyncData(() => getSiteSummary({ branchId: user.branchId }), [user.branchId])
  useEffect(() => {
    const t = setInterval(reload, 15000)
    return () => clearInterval(t)
  }, [reload])
  const badge = SYNC_BADGE[sync?.status ?? 'ONLINE']
  const BadgeIcon = badge.icon

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-ink/10 bg-card px-4 lg:px-8">
      <button type="button" onClick={onMenu} aria-label="Mở menu" className="cursor-pointer rounded-lg p-2 text-ink hover:bg-ink/5 lg:hidden">
        <Menu size={20} />
      </button>

      <div className="min-w-0">
        <p className="truncate text-sm font-bold text-ink">{branch ? branch.name : 'Toàn chuỗi'}</p>
        <p className="truncate text-xs text-ink/50">{branch ? `Chi nhánh ${branch.id}` : 'Dữ liệu tổng hợp từ Central'}</p>
      </div>

      <div className="ml-auto flex items-center gap-3">
        <span
          title={sync?.pending ? `${sync.pending} thay đổi đang chờ đồng bộ` : 'Không có thay đổi chờ đồng bộ'}
          className={`hidden items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold sm:inline-flex ${badge.cls}`}
        >
          <BadgeIcon size={12} /> {badge.text}
        </span>

        <Link to="/profile" className="flex items-center gap-2.5 rounded-lg px-2 py-1 hover:bg-ink/5">
          <span className="grid size-9 place-items-center rounded-full bg-accent text-sm font-bold text-white">{initial}</span>
          <span className="hidden text-left sm:block">
            <span className="block text-sm font-semibold leading-tight text-ink">{user.name}</span>
            <span className="block text-xs text-ink/50">{ROLE_LABEL[user.role]}</span>
          </span>
        </Link>

        <button type="button" onClick={handleLogout} aria-label="Đăng xuất" title="Đăng xuất" className="cursor-pointer rounded-lg p-2 text-ink/60 hover:bg-ink/5 hover:text-bad">
          <LogOut size={18} />
        </button>
      </div>
    </header>
  )
}