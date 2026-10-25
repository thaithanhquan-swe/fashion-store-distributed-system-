import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, LogIn, ShieldAlert, Shirt } from 'lucide-react'
import { Button, Field, Input } from '../../components/ui'
import { useAuth } from '../../hooks/useAuth'
import { BRANCHES } from '../../config/branches'
import { HOME, ROLE_LABEL } from '../../config/nav'

// Tài khoản có sẵn từ backend khi chạy local (mật khẩu chung 123456)
const DEMO = [
  { username: 'staff', role: 'staff', hint: 'Chi nhánh' },
  { username: 'manager', role: 'manager', hint: 'Chi nhánh' },
  { username: 'admin', role: 'admin', hint: 'Toàn chuỗi' },
]

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to={HOME[user.role]} replace />

  const submit = async (e) => {
    e.preventDefault()
    if (loading) return
    setError('')
    setLoading(true)
    try {
      const u = await login(username.trim(), password)
      navigate(HOME[u.role], { replace: true })
    } catch (err) {
      setError(err.message)
      setLoading(false)
    }
  }

  const fillDemo = (d) => {
    setUsername(d.username)
    setPassword('123456')
    setError('')
  }

  return (
    <div className="grid min-h-screen bg-bg lg:grid-cols-[1.1fr_1fr]">
      {/* Bên trái: giới thiệu, chỉ hiện trên màn hình rộng */}
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-ink p-12 text-white lg:flex">
        <div className="absolute -right-24 -top-24 size-96 rounded-full bg-accent/30 blur-3xl" />
        <div className="absolute -bottom-32 -left-16 size-96 rounded-full bg-accent/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-accent">
            <Shirt size={20} />
          </span>
          <span className="font-display text-xl font-bold">Fashion Store</span>
        </div>

        <div className="relative max-w-md">
          <h2 className="font-display text-4xl font-bold leading-tight">Một chuỗi, ba chi nhánh, một nhịp bán hàng.</h2>
          <p className="mt-4 text-white/60">
            Bán hàng, tồn kho và hóa đơn được ghi nhận tại từng cửa hàng, rồi tổng hợp về Central để quản trị theo dõi toàn chuỗi.
          </p>
        </div>

        <div className="relative flex flex-wrap gap-2">
          {BRANCHES.map((b) => (
            <span key={b.id} className="rounded-full border border-white/20 px-3 py-1 text-xs font-semibold text-white/80">
              {b.short}
            </span>
          ))}
          <span className="rounded-full bg-accent px-3 py-1 text-xs font-semibold">Central</span>
        </div>
      </aside>

      {/* Bên phải: form */}
      <main className="grid place-items-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <span className="grid size-9 place-items-center rounded-lg bg-accent text-white">
              <Shirt size={18} />
            </span>
            <span className="font-display text-lg font-bold text-ink">Fashion Store</span>
          </div>

          <h1 className="font-display text-3xl font-bold text-ink">Đăng nhập</h1>
          <p className="mt-1 text-sm text-ink/60">Dùng tài khoản do quản lý cấp.</p>

          <form onSubmit={submit} className="mt-6 space-y-4">
            <Field label="Tên đăng nhập">
              <Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus required />
            </Field>

            <Field label="Mật khẩu">
              <div className="relative">
                <Input
                  type={showPw ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  className="pr-10"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPw((v) => !v)}
                  aria-label={showPw ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  className="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer rounded p-1 text-ink/50 hover:text-ink"
                >
                  {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </Field>

            {error && (
              <div role="alert" className="flex items-center gap-2 rounded-lg bg-bad/10 px-3 py-2.5 text-sm font-medium text-bad">
                <ShieldAlert size={16} className="shrink-0" /> {error}
              </div>
            )}

            <Button type="submit" icon={LogIn} loading={loading} className="w-full">
              Đăng nhập
            </Button>
          </form>

          <div className="mt-8 border-t border-ink/10 pt-5">
            <p className="text-xs font-bold uppercase tracking-wide text-ink/50">Tài khoản có sẵn · mật khẩu 123456</p>
            <div className="mt-3 grid gap-2">
              {DEMO.map((d) => (
                <button
                  key={d.username}
                  type="button"
                  onClick={() => fillDemo(d)}
                  className="flex cursor-pointer items-center justify-between rounded-lg border border-ink/10 bg-card px-3 py-2 text-left transition hover:border-accent"
                >
                  <span>
                    <span className="block text-sm font-semibold text-ink">{ROLE_LABEL[d.role]}</span>
                    <span className="block text-xs text-ink/50">{d.username}</span>
                  </span>
                  <span className="text-xs font-semibold text-accent">{d.hint}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
