import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Button, Field, Input } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { HOME } from '../../config/nav'

// Bản đăng nhập cơ bản để chạy được khung. Bước sau sẽ làm lại giao diện chia đôi.
export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to={HOME[user.role]} replace />

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const u = await login(username, password)
      navigate(HOME[u.role], { replace: true })
    } catch (err) {
      setError(err.message)
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-screen place-items-center bg-bg p-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl bg-card p-6 shadow-lg">
        <h1 className="font-display text-2xl font-bold text-ink">Đăng nhập</h1>
        <Field label="Tên đăng nhập">
          <Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        </Field>
        <Field label="Mật khẩu" error={error}>
          <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Button type="submit" loading={loading} className="w-full">
          Đăng nhập
        </Button>
        <p className="text-xs text-ink/50">Demo: staff.q1 / manager.q1 / admin, mật khẩu 123456</p>
      </form>
    </div>
  )
}