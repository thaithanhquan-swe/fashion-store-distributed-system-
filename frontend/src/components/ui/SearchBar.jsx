import { Search, X } from 'lucide-react'

// Thanh tìm kiếm. onChange nhận thẳng chuỗi (không phải sự kiện).
// Các bộ lọc (Select...) đặt vào children, sẽ nằm bên phải ô tìm.
// <SearchBar value={q} onChange={setQ} placeholder="Tìm sản phẩm..."><Select .../></SearchBar>
export default function SearchBar({ value, onChange, placeholder = 'Tìm kiếm...', children }) {
  return (
    <div className="mb-4 flex flex-wrap items-center gap-3">
      <label className="flex h-10 min-w-56 max-w-md flex-1 items-center gap-2 rounded-lg border border-ink/15 bg-white px-3 text-ink/50 focus-within:border-accent focus-within:ring-3 focus-within:ring-accent/20">
        <Search size={16} />
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-ink/40"
        />
        {value && (
          <button type="button" onClick={() => onChange('')} aria-label="Xóa tìm kiếm" className="cursor-pointer">
            <X size={14} />
          </button>
        )}
      </label>
      {children}
    </div>
  )
}