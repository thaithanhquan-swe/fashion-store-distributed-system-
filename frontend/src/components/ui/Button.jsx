import { Loader2 } from 'lucide-react'

const VARIANTS = {
  primary: 'bg-accent text-white border-accent hover:opacity-90',
  secondary: 'bg-card text-ink border-ink/15 hover:bg-ink/5',
  ghost: 'bg-transparent text-ink border-transparent hover:bg-ink/5',
  danger: 'bg-bad text-white border-bad hover:opacity-90',
}

const SIZES = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
}

// <Button icon={Plus} variant="primary" loading={saving}>Thêm</Button>
export default function Button({
  variant = 'primary',
  size = 'md',
  icon: Icon,
  loading = false,
  disabled = false,
  className = '',
  children,
  ...rest
}) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      className={`inline-flex cursor-pointer items-center justify-center gap-2 whitespace-nowrap rounded-lg border font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
      {...rest}
    >
      {loading ? <Loader2 size={16} className="animate-spin" /> : Icon && <Icon size={16} />}
      {children}
    </button>
  )
}