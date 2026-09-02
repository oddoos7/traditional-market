import { MapPin, ShieldCheck, Store } from 'lucide-react'
import type { ReactNode } from 'react'

interface AuthLayoutProps {
  eyebrow: string
  title: string
  description: string
  children: ReactNode
}

export function AuthLayout({ eyebrow, title, description, children }: AuthLayoutProps) {
  return (
    <main className="auth-main">
      <section className="auth-shell" aria-labelledby="auth-page-title">
        <div className="auth-story" aria-label="서비스 소개">
          <div className="auth-story-pattern" aria-hidden="true" />
          <span className="auth-story-icon" aria-hidden="true">
            <Store size={34} strokeWidth={1.8} />
          </span>
          <p className="auth-story-kicker">Our neighborhood market</p>
          <h2>시장의 이야기를<br />내 동네와 더 가깝게</h2>
          <p className="auth-story-copy">
            익숙한 골목의 시장부터 새롭게 발견한 점포까지,
            로그인하고 우리 동네 시장 정보를 편하게 만나보세요.
          </p>
          <div className="auth-story-points">
            <span><MapPin size={16} aria-hidden="true" /> 시장·점포 정보 한눈에</span>
            <span><ShieldCheck size={16} aria-hidden="true" /> 안전한 세션 로그인</span>
          </div>
        </div>

        <div className="auth-form-panel">
          <div className="auth-heading">
            <span className="eyebrow">{eyebrow}</span>
            <h1 id="auth-page-title">{title}</h1>
            <p>{description}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  )
}
