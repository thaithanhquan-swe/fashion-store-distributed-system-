import { NavLink } from 'react-router-dom'
import { Shirt, X } from 'lucide-react'
import { NAV } from '../../config/nav'

// Menu bên trái, lấy từ config/nav.js theo role.
// Màn hình nhỏ: trượt ra từ trái, `open` + `onClose` điều khiển việc đóng mở.
export default function Sidebar({ role, open, onClose }) {
  const items = NAV[role] || []

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-ink/50 lg:hidden" onClick={onClose} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-ink text-white transition-transform lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="flex h-16 items-center justify-between px-5">
          <div className="flex items-center gap-2.5">
            <span className="grid size-9 place-items-center rounded-lg bg-accent">
              <Shirt size={18} />
            </span>
            <span className="font-display text-lg font-bold">Fashion Store</span>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng menu" className="cursor-pointer rounded-lg p-1.5 text-white/70 hover:bg-white/10 lg:hidden">
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 pb-6">
          {items.map((item, i) =>
            item.group ? (
              <p key={item.group + i} className="px-3 pb-1 pt-5 text-[11px] font-bold uppercase tracking-wider text-white/40">
                {item.group}
              </p>
            ) : (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) =>
                  `mt-0.5 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                    isActive ? 'bg-accent text-white' : 'text-white/70 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                <item.icon size={18} />
                {item.label}
              </NavLink>
            ),
          )}
        </nav>
      </aside>
    </>
  )
}