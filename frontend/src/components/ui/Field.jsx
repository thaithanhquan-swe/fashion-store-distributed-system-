// Bọc một ô nhập: nhãn phía trên, gợi ý hoặc lỗi phía dưới.
// <Field label="Họ tên" error={errors.name}><Input value={...} onChange={...} /></Field>
export default function Field({ label, hint, error, children }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-bold text-ink/80">{label}</span>
      {children}
      {error ? (
        <span className="text-xs font-medium text-bad">{error}</span>
      ) : (
        hint && <span className="text-xs text-ink/50">{hint}</span>
      )}
    </label>
  )
}