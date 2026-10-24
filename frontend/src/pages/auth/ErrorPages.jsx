import { useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { HOME, ROLE_LABEL } from '../../config/nav'

function ErrorScreen({ code, title, description }) {
  const { user } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="grid place-items-center py-16 text-center">
      <p className="font-display text-8xl font-extrabold text-accent/20">{code}</p>
      <h1 className="mt-2 font-display text-2xl font-bold text-ink">{title}</h1>
      <p className="mt-2 max-w-md text-sm text-ink/60">{description(user)}</p>
      <Button className="mt-6" onClick={() => navigate(HOME[user.role], { replace: true })}>
        Về trang chủ của tôi
      </Button>
    </div>
  )
}

export function ForbiddenPage() {
  return (
    <ErrorScreen
      code="403"
      title="Bạn không có quyền truy cập"
      description={(user) => `Trang này dành cho vai trò khác. Vai trò hiện tại của bạn: ${ROLE_LABEL[user.role]}.`}
    />
  )
}

export function NotFoundPage() {
  return (
    <ErrorScreen
      code="404"
      title="Không tìm thấy trang"
      description={() => 'Đường dẫn không tồn tại hoặc đã được di chuyển.'}
    />
  )
}