// Ba chi nhánh cố định (theo bảng ChiNhanh)
export const BRANCHES = [
  { id: 'CN01', name: 'Store Quận 1', short: 'Quận 1' },
  { id: 'CN02', name: 'Store Quận 7', short: 'Quận 7' },
  { id: 'CN03', name: 'Store Thủ Đức', short: 'Thủ Đức' },
]

export const getBranch = (id) => BRANCHES.find((b) => b.id === id)