const TONES = {
  accent: 'bg-accent/15 text-accent',
  ok: 'bg-ok/15 text-ok',
  warn: 'bg-warn/15 text-warn',
  bad: 'bg-bad/15 text-bad',
}

// <StatCard icon={Wallet} label="Doanh thu hôm nay" value="4.215.000 ₫" hint="9 hóa đơn" />
export default function StatCard({ icon: Icon, label, value, hint, tone = 'accent' }) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-ink/10 bg-card p-4">
      {Icon && (
        <span className={`grid size-11 shrink-0 place-items-center rounded-xl ${TONES[tone]}`}>
          <Icon size={20} />
        </span>
      )}
      <div className="min-w-0">
        <div className="text-xs font-semibold text-ink/60">{label}</div>
        <div className="font-display text-2xl font-bold tabular-nums text-ink">{value}</div>
        {hint && <div className="text-xs text-ink/50">{hint}</div>}
      </div>
    </div>
  )
}