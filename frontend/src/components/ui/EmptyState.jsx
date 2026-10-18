import { Inbox } from 'lucide-react'

// Hiển thị khi danh sách rỗng hoặc có lỗi
export default function EmptyState({ icon: Icon = Inbox, title = 'Chưa có dữ liệu', description, action }) {
  return (
    <div className="grid place-items-center gap-2 p-10 text-center">
      <Icon size={32} strokeWidth={1.5} className="text-ink/30" />
      <div className="font-semibold text-ink">{title}</div>
      {description && <p className="max-w-sm text-sm text-ink/60">{description}</p>}
      {action}
    </div>
  )
}