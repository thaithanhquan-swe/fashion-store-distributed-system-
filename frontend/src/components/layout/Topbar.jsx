import { Link, useNavigate } from 'react-router-dom'
import { LogOut, Menu, RefreshCw } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { getBranch } from '../../config/branches'
import { ROLE_LABEL } from '../../config/nav'

// Thanh trên cùng: chi nhánh đang làm việc, trạng thái đồng bộ Central, tài khoản.
export default function Topbar({ onMenu }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const branch = getBranch(user.branchId)
  const initial = user.name.trim().split(' ').pop()[0]

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
        {/* Tạm thời hiển thị cố định, sau này lấy từ API trạng thái site */}
        <span className="hidden items-center gap-1.5 rounded-full bg-ok/15 px-3 py-1 text-xs font-semibold text-ok sm:inline-flex">
          <RefreshCw size={12} /> Đã đồng bộ Central
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