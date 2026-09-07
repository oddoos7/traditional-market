import { LoaderCircle, LockKeyhole } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function AdminRoute({ children }: { children: ReactNode }) {
  const location = useLocation()
  const { user, isCheckingAuth } = useAuth()

  if (isCheckingAuth) {
    return (
      <main className="auth-route-loading" aria-label="관리자 권한을 확인하는 중">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
      </main>
    )
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname, message: '관리자 화면은 로그인 후 이용할 수 있습니다.' }}
      />
    )
  }

  if (user.role !== 'ADMIN') {
    return (
      <main className="admin-access-main">
        <section className="admin-access-card" role="alert" aria-labelledby="access-denied-title">
          <span className="admin-access-icon" aria-hidden="true">
            <LockKeyhole size={30} />
          </span>
          <span className="eyebrow">403 · Access denied</span>
          <h1 id="access-denied-title">관리자 권한이 필요합니다</h1>
          <p>
            현재 로그인한 계정은 일반 회원입니다.
            시장 등록과 수정은 관리자 계정으로만 이용할 수 있어요.
          </p>
          <Link to="/">시장 목록으로 돌아가기</Link>
        </section>
      </main>
    )
  }

  return children
}
