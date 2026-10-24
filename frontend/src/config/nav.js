import {
  Boxes, ChartColumn, GitCompareArrows, IdCard, KeyRound, LayoutDashboard,
  Network, PackageMinus, Receipt, Shirt, ShoppingBag, Store, Trophy,
  UserRoundCheck, Users,
} from 'lucide-react'

// Menu theo role. Mục có `group` là tiêu đề nhóm trong sidebar.
export const NAV = {
  staff: [
    { to: '/pos', label: 'Bán hàng', icon: ShoppingBag },
    { to: '/staff/products', label: 'Sản phẩm', icon: Shirt },
    { to: '/staff/inventory', label: 'Tồn kho', icon: Boxes },
    { to: '/staff/customers', label: 'Khách hàng', icon: Users },
    { to: '/staff/invoices', label: 'Hóa đơn', icon: Receipt },
  ],
  manager: [
    { to: '/manager/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { to: '/pos', label: 'Bán hàng', icon: ShoppingBag },
    { group: 'Kho và sản phẩm' },
    { to: '/manager/products', label: 'Sản phẩm', icon: Shirt },
    { to: '/manager/inventory', label: 'Tồn kho', icon: Boxes },
    { to: '/manager/low-stock', label: 'Sắp hết hàng', icon: PackageMinus },
    { group: 'Kinh doanh' },
    { to: '/manager/customers', label: 'Khách hàng', icon: Users },
    { to: '/manager/invoices', label: 'Hóa đơn', icon: Receipt },
    { to: '/manager/statistics', label: 'Thống kê', icon: ChartColumn },
    { group: 'Nhân sự' },
    { to: '/manager/employees', label: 'Nhân viên', icon: IdCard },
    { to: '/manager/accounts', label: 'Tài khoản', icon: KeyRound },
  ],
  admin: [
    { to: '/admin/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { to: '/admin/branches', label: 'Chi nhánh', icon: Store },
    { group: 'Kho và sản phẩm' },
    { to: '/admin/products', label: 'Sản phẩm', icon: Shirt },
    { to: '/admin/inventory', label: 'Tồn kho', icon: Boxes },
    { group: 'Kinh doanh' },
    { to: '/admin/customers', label: 'Khách hàng', icon: Users },
    { to: '/admin/invoices', label: 'Hóa đơn', icon: Receipt },
    { group: 'Nhân sự' },
    { to: '/admin/employees', label: 'Nhân viên', icon: IdCard },
    { to: '/admin/accounts', label: 'Tài khoản', icon: KeyRound },
    { group: 'Thống kê' },
    { to: '/admin/statistics', label: 'Thống kê chuỗi', icon: ChartColumn },
    { to: '/admin/revenue', label: 'So sánh doanh thu', icon: GitCompareArrows },
    { to: '/admin/top-products', label: 'Bán chạy', icon: Trophy },
    { to: '/admin/employee-sales', label: 'Doanh số nhân viên', icon: UserRoundCheck },
    { group: 'Hệ thống' },
    { to: '/admin/sites', label: 'Trạng thái Site', icon: Network },
  ],
}

// Trang chủ của từng role sau khi đăng nhập
export const HOME = {
  staff: '/pos',
  manager: '/manager/dashboard',
  admin: '/admin/dashboard',
}

export const ROLE_LABEL = { staff: 'Nhân viên', manager: 'Quản lý', admin: 'Quản trị viên' }