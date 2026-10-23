// Dữ liệu giả cho các API backend chưa có (tồn kho, hóa đơn, thống kê).
// Theo quy ước dự án: khi backend chưa có API thì service trả dữ liệu giả,
// sau này chỉ thay phần thân hàm trong service, trang giao diện không phải sửa.
//
// Tên trường trong file này bám theo tên cột của bảng SQL Server:
// TonKho (MaChiNhanh, MaSanPham, SoLuongTon, MucTonToiThieu, NgayCapNhat)
// HoaDon (MaHoaDon, MaChiNhanh, MaKhachHang, MaNhanVien, NgayLap, TongTien, PhuongThucTT, TrangThai)
// ChiTietHoaDon (MaHoaDon, MaSanPham, SoLuong, DonGia, GiamGia, ThanhTien)

export const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// Số ngẫu nhiên ổn định: cùng seed luôn cho cùng kết quả, không nhảy số mỗi lần tải trang.
const pseudo = (seed) => {
  const x = Math.sin(seed * 12.9898) * 43758.5453
  return x - Math.floor(x)
}

export const BRANCH_IDS = ['CN01', 'CN02', 'CN03']

export const PRODUCTS = [
  { productId: 'SP001', productName: 'Áo khoác dạ mùa thu', category: 'Áo', brand: 'Luna', color: 'Be', size: 'M', price: 890000 },
  { productId: 'SP002', productName: 'Áo sơ mi linen', category: 'Áo', brand: 'Luna', color: 'Trắng', size: 'S', price: 420000 },
  { productId: 'SP003', productName: 'Áo thun basic cotton', category: 'Áo', brand: 'Nova', color: 'Đen', size: 'M', price: 180000 },
  { productId: 'SP004', productName: 'Quần jeans ống rộng', category: 'Quần', brand: 'Nova', color: 'Xanh', size: 'L', price: 650000 },
  { productId: 'SP005', productName: 'Quần tây công sở', category: 'Quần', brand: 'Meridian', color: 'Đen', size: 'M', price: 720000 },
  { productId: 'SP006', productName: 'Váy midi hoa nhẹ', category: 'Váy', brand: 'Mira', color: 'Hồng', size: 'S', price: 560000 },
  { productId: 'SP007', productName: 'Váy ôm dạ dài', category: 'Váy', brand: 'Mira', color: 'Kem', size: 'M', price: 630000 },
  { productId: 'SP008', productName: 'Giày da công sở', category: 'Giày', brand: 'Meridian', color: 'Nâu', size: '39', price: 1150000 },
  { productId: 'SP009', productName: 'Giày sneakers trắng', category: 'Giày', brand: 'Nova', color: 'Trắng', size: '40', price: 980000 },
  { productId: 'SP010', productName: 'Túi xách da tay', category: 'Phụ kiện', brand: 'Luna', color: 'Nâu', size: 'M', price: 1450000 },
  { productId: 'SP011', productName: 'Khăn lụa thêu hoa', category: 'Phụ kiện', brand: 'Mira', color: 'Đỏ', size: 'M', price: 260000 },
  { productId: 'SP012', productName: 'Thắt lưng da mềm', category: 'Phụ kiện', brand: 'Meridian', color: 'Đen', size: 'M', price: 390000 },
]

export const EMPLOYEES = [
  { employeeId: 'NV001', branchId: 'CN01', employeeName: 'Nguyễn Minh Khoa', position: 'Quản lý chi nhánh' },
  { employeeId: 'NV002', branchId: 'CN01', employeeName: 'Nguyễn Thị Mai', position: 'Nhân viên bán hàng' },
  { employeeId: 'NV003', branchId: 'CN01', employeeName: 'Trần Quốc Bảo', position: 'Nhân viên bán hàng' },
  { employeeId: 'NV004', branchId: 'CN02', employeeName: 'Lê Thu Hà', position: 'Quản lý chi nhánh' },
  { employeeId: 'NV005', branchId: 'CN02', employeeName: 'Phạm Anh Tuấn', position: 'Nhân viên bán hàng' },
  { employeeId: 'NV006', branchId: 'CN02', employeeName: 'Võ Thị Lan', position: 'Nhân viên bán hàng' },
  { employeeId: 'NV007', branchId: 'CN03', employeeName: 'Đặng Hải Đăng', position: 'Quản lý chi nhánh' },
  { employeeId: 'NV008', branchId: 'CN03', employeeName: 'Bùi Kim Ngân', position: 'Nhân viên bán hàng' },
  { employeeId: 'NV009', branchId: 'CN03', employeeName: 'Hoàng Gia Huy', position: 'Nhân viên bán hàng' },
]

const CUSTOMERS = [
  { customerId: 'KH001', fullName: 'Trần Thị Hồng Nhung', phoneNumber: '0901000001' },
  { customerId: 'KH002', fullName: 'Lê Văn Hùng', phoneNumber: '0901000002' },
  { customerId: 'KH003', fullName: 'Phạm Quốc Bảo', phoneNumber: '0901000003' },
  { customerId: 'KH004', fullName: 'Hoàng Thị Mai Anh', phoneNumber: '0901000004' },
  { customerId: 'KH005', fullName: 'Vũ Đình Trung', phoneNumber: '0901000005' },
  { customerId: 'KH006', fullName: 'Đỗ Thuỳ Linh', phoneNumber: '0901000006' },
  { customerId: 'KH007', fullName: 'Ngô Gia Bách', phoneNumber: '0901000007' },
  { customerId: 'KH008', fullName: 'Dương Mỹ Yến', phoneNumber: '0901000008' },
]

// Tồn kho theo chi nhánh: [mã sản phẩm, số lượng tồn, mức tồn tối thiểu]
const INVENTORY_SEED = {
  CN01: [
    ['SP001', 10, 5], ['SP002', 4, 5], ['SP003', 0, 5], ['SP004', 12, 6],
    ['SP005', 3, 4], ['SP006', 7, 3], ['SP007', 2, 3], ['SP008', 5, 2],
    ['SP009', 15, 6], ['SP010', 1, 2], ['SP011', 9, 4], ['SP012', 6, 3],
  ],
  CN02: [
    ['SP001', 8, 5], ['SP002', 14, 5], ['SP003', 22, 5], ['SP004', 0, 6],
    ['SP005', 9, 4], ['SP006', 3, 3], ['SP007', 11, 3], ['SP008', 6, 2],
    ['SP009', 4, 6], ['SP010', 8, 2], ['SP011', 2, 4], ['SP012', 7, 3],
  ],
  CN03: [
    ['SP001', 16, 5], ['SP002', 2, 5], ['SP003', 19, 5], ['SP004', 13, 6],
    ['SP005', 5, 4], ['SP006', 9, 3], ['SP007', 0, 3], ['SP008', 3, 2],
    ['SP009', 12, 6], ['SP010', 6, 2], ['SP011', 7, 4], ['SP012', 1, 3],
  ],
}

const PRODUCT_MAP = new Map(PRODUCTS.map((p) => [p.productId, p]))

const buildInventory = () => {
  const now = Date.now()
  const rows = []
  for (const [branchId, items] of Object.entries(INVENTORY_SEED)) {
    items.forEach(([productId, quantity, minQuantity], index) => {
      rows.push({
        key: `${branchId}-${productId}`,
        branchId,
        productId,
        quantity,
        minQuantity,
        updatedAt: new Date(now - (index + 1) * 3 * 60 * 60 * 1000).toISOString(),
      })
    })
  }
  return rows
}

export const INVENTORY = buildInventory()

const PAYMENT_METHODS = ['TIEN_MAT', 'THE', 'CHUYEN_KHOAN']
const INVOICE_STATUS = 'DA_THANH_TOAN'
const HISTORY_DAYS = 90

// Sinh lịch sử bán hàng 90 ngày gần nhất cho ba chi nhánh.
const buildInvoices = () => {
  const invoices = []
  const details = []
  const sequence = { CN01: 0, CN02: 0, CN03: 0 }
  const today = new Date()
  today.setHours(23, 59, 59, 999)

  for (let day = HISTORY_DAYS - 1; day >= 0; day--) {
    for (let b = 0; b < BRANCH_IDS.length; b++) {
      const branchId = BRANCH_IDS[b]
      const branchProducts = INVENTORY.filter((r) => r.branchId === branchId).map((r) => r.productId)
      const branchStaff = EMPLOYEES.filter((e) => e.branchId === branchId)
      const invoiceCount = Math.floor(pseudo(day * 13 + b * 7 + 3) * 3)

      for (let k = 0; k < invoiceCount; k++) {
        const seed = day * 100 + b * 10 + k
        sequence[branchId] += 1
        const invoiceId = `HD${b + 1}${String(sequence[branchId]).padStart(4, '0')}`

        const createdAt = new Date(today)
        createdAt.setDate(createdAt.getDate() - day)
        createdAt.setHours(8 + Math.floor(pseudo(seed + 1) * 12), Math.floor(pseudo(seed + 2) * 60), 0, 0)

        const employee = branchStaff[Math.floor(pseudo(seed + 3) * branchStaff.length)]
        const customer = CUSTOMERS[Math.floor(pseudo(seed + 4) * CUSTOMERS.length)]
        const lineCount = 1 + Math.floor(pseudo(seed + 5) * 3)

        let totalAmount = 0
        const lines = []
        for (let l = 0; l < lineCount; l++) {
          const productId = branchProducts[Math.floor(pseudo(seed + 10 + l) * branchProducts.length)]
          const product = PRODUCT_MAP.get(productId)
          const quantity = 1 + Math.floor(pseudo(seed + 20 + l) * 3)
          const discount = pseudo(seed + 30 + l) > 0.75 ? Math.round(product.price * 0.1) : 0
          const lineTotal = quantity * product.price - discount
          totalAmount += lineTotal
          lines.push({
            invoiceId,
            productId,
            productName: product.productName,
            quantity,
            unitPrice: product.price,
            discount,
            lineTotal,
          })
        }

        invoices.push({
          invoiceId,
          branchId,
          customerId: customer.customerId,
          customerName: customer.fullName,
          employeeId: employee.employeeId,
          createdAt: createdAt.toISOString(),
          totalAmount,
          paymentMethod: PAYMENT_METHODS[Math.floor(pseudo(seed + 40) * PAYMENT_METHODS.length)],
          status: INVOICE_STATUS,
        })
        details.push(...lines)
      }
    }
  }
  return { invoices, details }
}

const generated = buildInvoices()

export const INVOICES = generated.invoices
export const INVOICE_DETAILS = generated.details
