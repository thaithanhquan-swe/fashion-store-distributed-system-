// <Card title="Doanh thu" action={<Button size="sm">Xem</Button>}>...</Card>
export default function Card({ title, action, padded = true, className = '', children }) {
  return (
    <section className={`min-w-0 rounded-xl border border-ink/10 bg-card ${className}`}>
      {(title || action) && (
        <header className="flex items-center justify-between gap-3 px-5 pt-4">
          <h2 className="text-sm font-bold text-ink">{title}</h2>
          {action}
        </header>
      )}
      <div className={padded ? 'px-5 pb-5 pt-4' : ''}>{children}</div>
    </section>
  )
}