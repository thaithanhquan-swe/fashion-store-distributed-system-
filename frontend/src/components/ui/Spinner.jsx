import { Loader2 } from 'lucide-react'

// Hiển thị khi đang tải dữ liệu
export default function Spinner({ label = 'Đang tải...' }) {
  return (
    <div className="flex items-center justify-center gap-2 p-10 text-sm text-ink/60">
      <Loader2 size={20} className="animate-spin" /> {label}
    </div>
  )
}