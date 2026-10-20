import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

// Chặn truy cập theo đăng nhập và vai trò.
// <Route element={<RoleGuard />}>                      → chỉ cần đã đăng nhập
// <Route element={<RoleGuard allow={['admin']} />}>    → chỉ admin
export default function RoleGuard({ allow }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (allow && !allow.includes(user.role)) return <Navigate to="/403" replace />
  return <Outlet />
}