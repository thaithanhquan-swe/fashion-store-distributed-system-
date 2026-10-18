const TONES = {
  neutral: 'bg-ink/10 text-ink/70',
  ok: 'bg-ok/15 text-ok',
  warn: 'bg-warn/15 text-warn',
  bad: 'bg-bad/15 text-bad',
  accent: 'bg-accent/15 text-accent',
}

// <Badge tone="warn">Sắp hết</Badge>
export default function Badge({ tone = 'neutral', children }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${TONES[tone]}`}>
      {children}
    </span>
  )
}