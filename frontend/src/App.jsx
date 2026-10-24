import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import { AppLayout } from './components/layout'
import RoleGuard from './routes/RoleGuard'
import { HOME, NAV } from './config/nav'
import Placeholder from './pages/Placeholder'
import LoginPage from './pages/auth/LoginPage'
import ProfilePage from './pages/auth/ProfilePage'
import { ForbiddenPage, NotFoundPage } from './pages/auth/ErrorPages'
import EmployeeListPage from './pages/employees/EmployeeListPage'
import AccountListPage from './pages/accounts/AccountListPage'
import BranchListPage from './pages/admin/BranchListPage'
import BranchDetailPage from './pages/admin/BranchDetailPage'
import SiteStatusPage from './pages/admin/SiteStatusPage'
import ProductListPage from './pages/products/ProductListPage'
import CustomerListPage from './pages/customers/CustomerListPage'
import CustomerDetailPage from './pages/customers/CustomerDetailPage'
import InvoiceListPage from './pages/invoices/InvoiceListPage'
import InvoiceDetailPage from './pages/invoices/InvoiceDetailPage'
import PosPage from './pages/pos/PosPage'
import InventoryPage from './pages/inventory/InventoryPage'
import LowStockPage from './pages/inventory/LowStockPage'
import AdminDashboardPage from './pages/dashboard/AdminDashboardPage'
import ManagerDashboardPage from './pages/dashboard/ManagerDashboardPage'
import StatisticsPage from './pages/statistics/StatisticsPage'
import RevenueComparisonPage from './pages/statistics/RevenueComparisonPage'
import TopProductsPage from './pages/statistics/TopProductsPage'
import EmployeeSalesPage from './pages/statistics/EmployeeSalesPage'

// ===== CHỖ CẮM TRANG =====
// Làm xong trang nào thì import rồi thêm một dòng vào đây: '/đường-dẫn': <TênTrang />
// Đường dẫn lấy đúng từ src/config/nav.js. Trang nào chưa có sẽ hiện Placeholder.
const PAGES = {
  // Nhân viên: dùng chung cho manager (chi nhánh mình) và admin (toàn chuỗi).
  '/manager/employees': <EmployeeListPage />,
  '/admin/employees': <EmployeeListPage />,

  // Tài khoản: manager (chi nhánh mình) và admin (toàn chuỗi).
  '/manager/accounts': <AccountListPage />,
  '/admin/accounts': <AccountListPage />,

  // Khách hàng: dùng chung cho cả 3 vai trò.
  '/staff/customers': <CustomerListPage />,
  '/manager/customers': <CustomerListPage />,
  '/admin/customers': <CustomerListPage />,

    // Bán hàng (staff và manager dùng chung đường dẫn /pos).
  '/pos': <PosPage />,

    // Hóa đơn: dùng chung, mỗi vai trò thấy phạm vi của mình.
  '/staff/invoices': <InvoiceListPage />,
  '/manager/invoices': <InvoiceListPage />,
  '/admin/invoices': <InvoiceListPage />,

  // Sản phẩm: staff và manager chỉ xem, admin quản lý danh mục.
  '/staff/products': <ProductListPage />,
  '/manager/products': <ProductListPage />,
  '/admin/products': <ProductListPage />,

  // Chi nhánh (admin).
  '/admin/branches': <BranchListPage />,
    '/admin/sites': <SiteStatusPage />,

  // Tồn kho: dùng chung cho staff, manager và admin.
  '/staff/inventory': <InventoryPage />,
  '/manager/inventory': <InventoryPage />,
  '/admin/inventory': <InventoryPage />,
  '/manager/low-stock': <LowStockPage />,

  // Tổng quan và thống kê chuỗi.
  '/manager/dashboard': <ManagerDashboardPage />,
  '/admin/dashboard': <AdminDashboardPage />,
  '/manager/statistics': <StatisticsPage />,
  '/admin/statistics': <StatisticsPage />,
  '/admin/revenue': <RevenueComparisonPage />,
  '/admin/top-products': <TopProductsPage />,
  '/admin/employee-sales': <EmployeeSalesPage />,
}

// Trang con không có trong menu (chi tiết, ...). Thêm một dòng: { path, roles, element }.
// path có thể có tham số, ví dụ '/admin/branches/:branchId'.
const EXTRA_PAGES = [
  { path: '/admin/branches/:branchId', roles: ['admin'], element: <BranchDetailPage /> },
  { path: '/staff/customers/:customerId', roles: ['staff'], element: <CustomerDetailPage /> },
  { path: '/manager/customers/:customerId', roles: ['manager'], element: <CustomerDetailPage /> },
  { path: '/admin/customers/:customerId', roles: ['admin'], element: <CustomerDetailPage /> },
  { path: '/staff/invoices/:invoiceId', roles: ['staff'], element: <InvoiceDetailPage /> },
  { path: '/manager/invoices/:invoiceId', roles: ['manager'], element: <InvoiceDetailPage /> },
  { path: '/admin/invoices/:invoiceId', roles: ['admin'], element: <InvoiceDetailPage /> },
]

// Gom đường dẫn từ nav.js: mỗi đường dẫn được những role nào vào
const ROUTES = {}
for (const [role, items] of Object.entries(NAV)) {
  for (const item of items) {
    if (!item.to) continue
    const entry = (ROUTES[item.to] ??= { label: item.label, roles: [] })
    entry.roles.push(role)
  }
}

function RedirectHome() {
  const { user } = useAuth()
  return <Navigate to={user ? HOME[user.role] : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<RedirectHome />} />

          <Route element={<RoleGuard />}>
            <Route element={<AppLayout />}>
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/403" element={<ForbiddenPage />} />

              {Object.entries(ROUTES).map(([path, { label, roles }]) => (
                <Route key={path} element={<RoleGuard allow={roles} />}>
                  <Route path={path} element={PAGES[path] ?? <Placeholder title={label} />} />
                </Route>
              ))}

              {EXTRA_PAGES.map(({ path, roles, element }) => (
                <Route key={path} element={<RoleGuard allow={roles} />}>
                  <Route path={path} element={element} />
                </Route>
              ))}

              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}