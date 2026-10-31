// Danh sách 3 chi nhánh chính của hệ thống phân tán
// Mã chi nhánh: CN1 (Quận 1), CN2 (Quận 7), CN3 (Thủ Đức) — khớp PDF filter MACN='CN1'
export const BRANCHES = [
  { id: 'CN1', name: 'Store Quận 1', short: 'Quận 1' },
  { id: 'CN2', name: 'Store Quận 7', short: 'Quận 7' },
  { id: 'CN3', name: 'Store Thủ Đức', short: 'Thủ Đức' },
]

export const getBranch = (id) => {
  if (!id) return null
  const trimmed = id.trim()
  const found =
    BRANCHES.find((b) => b.id.toLowerCase() === trimmed.toLowerCase()) ||
    BRANCHES.find(
      (b) =>
        b.id.replace(/^CN0+/, 'CN').toLowerCase() ===
        trimmed.replace(/^CN0+/, 'CN').toLowerCase()
    )
  return found ?? { id: trimmed, name: trimmed, short: trimmed }
}