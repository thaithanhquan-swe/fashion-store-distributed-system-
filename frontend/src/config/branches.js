// Danh sách 3 chi nhánh chính của hệ thống phân tán
export const BRANCHES = [
  { id: 'CN001', name: 'Store Quận 1', short: 'Quận 1' },
  { id: 'CN002', name: 'Store Quận 3', short: 'Quận 3' },
  { id: 'CN003', name: 'Store Thủ Đức', short: 'Thủ Đức' },
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