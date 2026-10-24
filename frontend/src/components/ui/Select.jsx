import { ChevronDown } from 'lucide-react'

// <Select value={cat} onChange={(e) => setCat(e.target.value)}
//         options={[{ value: '', label: 'Mọi loại' }, { value: 'Áo', label: 'Áo' }]} />
export default function Select({ options, className = '', ...rest }) {
  return (
    <div className={`relative ${className}`}>
      <select
        className="h-10 w-full cursor-pointer appearance-none rounded-lg border border-ink/15 bg-white pl-3 pr-9 text-sm text-ink focus:border-accent focus:outline-none focus:ring-3 focus:ring-accent/20"
        {...rest}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <ChevronDown size={16} className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-ink/50" />
    </div>
  )
}