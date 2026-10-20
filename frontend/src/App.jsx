import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import { AppLayout } from './components/layout'
import RoleGuard from './routes/RoleGuard'
import { HOME, NAV } from './config/nav'
import Placeholder from './pages/Placeholder'
import LoginPage from './pages/auth/LoginPage'

// ===== CHỖ CẮM TRANG =====
// Làm xong trang nào thì import rồi thêm một dòng vào đây: '/đường-dẫn': <TênTrang />
// Đường dẫn lấy đúng từ src/config/nav.js. Trang nào chưa có sẽ hiện Placeholder.
const PAGES = {
  // '/admin/employees': <EmployeesPage />,
}

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
              <Route path="/profile" element={<Placeholder title="Thông tin tài khoản" />} />
              <Route path="/403" element={<Placeholder title="403 - Không có quyền truy cập" />} />

              {Object.entries(ROUTES).map(([path, { label, roles }]) => (
                <Route key={path} element={<RoleGuard allow={roles} />}>
                  <Route path={path} element={PAGES[path] ?? <Placeholder title={label} />} />
                </Route>
              ))}

              <Route path="*" element={<Placeholder title="404 - Không tìm thấy trang" />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}