import { LoaderCircle } from 'lucide-react'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function RequireAuthRoute({ children }: { children: ReactNode }) {
  const location = useLocation()
  const { user, isCheckingAuth } = useAuth()

  if (isCheckingAuth) {
    return (
      <main className="auth-route-loading" aria-label="로그인 상태를 확인하는 중">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
      </main>
    )
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname,
          message: '찜한 점포는 로그인 후 확인할 수 있습니다.',
        }}
      />
    )
  }

  return children
}
