// Ô nhập chữ/số/ngày. Dùng như <input>: nhận value, onChange (sự kiện gốc), type, required...
export default function Input({ error = false, className = '', ...rest }) {
  return (
    <input
      className={`h-10 w-full rounded-lg border bg-white px-3 text-sm text-ink placeholder:text-ink/40 focus:border-accent focus:outline-none focus:ring-3 focus:ring-accent/20 disabled:bg-ink/5 disabled:text-ink/50 ${
        error ? 'border-bad' : 'border-ink/15'
      } ${className}`}
      {...rest}
    />
  )
}