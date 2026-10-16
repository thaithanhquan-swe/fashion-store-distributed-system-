import { Shirt } from 'lucide-react'

export default function App() {
  return (
    <div className="min-h-screen bg-bg p-8">
      <div className="rounded-xl bg-card p-6 shadow-sm">
        <h1 className="flex items-center gap-2 font-display text-2xl text-accent">
          <Shirt size={22} /> Fashion Store
        </h1>
        <p className="mt-2 text-ok">Môi trường đã sẵn sàng</p>
      </div>
    </div>
  )
}