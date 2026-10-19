import { Construction } from 'lucide-react'
import { EmptyState } from '../components/ui'

// Trang tạm cho những màn hình chưa làm. Ai làm trang nào thì thay trong App.jsx.
export default function Placeholder({ title }) {
  return (
    <div>
      <h1 className="font-display text-2xl font-bold text-ink">{title}</h1>
      <div className="mt-6 rounded-xl border border-dashed border-ink/20 bg-card">
        <EmptyState icon={Construction} title="Trang đang được xây dựng" />
      </div>
    </div>
  )
}