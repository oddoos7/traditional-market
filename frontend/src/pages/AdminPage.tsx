import { useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  BadgeCheck,
  Database,
  KeyRound,
  LayoutDashboard,
  ShieldCheck,
  Store,
} from 'lucide-react'
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getMarkets } from '../api/marketApi'
import { useAuth } from '../auth/AuthContext'

export function AdminPage() {
  const { user } = useAuth()
  const marketCountQuery = useQuery({
    queryKey: ['markets', 'admin-summary'],
    queryFn: () => getMarkets({
      keyword: '',
      status: '',
      page: 0,
      size: 1,
      sort: 'LATEST',
    }),
  })

  useEffect(() => {
    document.title = '관리자 센터 | 우리동네 전통시장'
  }, [])

  return (
    <main className="admin-main">
      <section className="admin-hero" aria-labelledby="admin-page-title">
        <div>
          <span className="eyebrow">Administration</span>
          <h1 id="admin-page-title">관리자 센터</h1>
          <p>
            안녕하세요, <strong>{user?.name}</strong>님.<br />
            전통시장 정보를 안전하게 관리할 수 있는 전용 공간입니다.
          </p>
        </div>
        <div className="admin-identity-card">
          <span className="admin-identity-icon" aria-hidden="true">
            <ShieldCheck size={27} />
          </span>
          <div>
            <span>현재 접근 권한</span>
            <strong>ADMIN</strong>
            <small>{user?.email}</small>
          </div>
          <BadgeCheck size={22} aria-label="권한 확인 완료" />
        </div>
      </section>

      <section className="admin-summary" aria-labelledby="admin-summary-title">
        <div className="admin-section-heading">
          <LayoutDashboard size={19} aria-hidden="true" />
          <h2 id="admin-summary-title">운영 현황</h2>
        </div>
        <div className="admin-summary-grid">
          <article className="admin-stat-card">
            <span><Store size={20} aria-hidden="true" /></span>
            <div>
              <small>등록된 시장</small>
              <strong>
                {marketCountQuery.isLoading
                  ? '확인 중'
                  : marketCountQuery.isError
                    ? '조회 실패'
                    : `${marketCountQuery.data?.totalElements ?? 0}곳`}
              </strong>
            </div>
          </article>
          <article className="admin-stat-card">
            <span><KeyRound size={20} aria-hidden="true" /></span>
            <div>
              <small>시장 쓰기 권한</small>
              <strong>허용됨</strong>
            </div>
          </article>
          <article className="admin-stat-card">
            <span><Database size={20} aria-hidden="true" /></span>
            <div>
              <small>인증 방식</small>
              <strong>서버 세션</strong>
            </div>
          </article>
        </div>
      </section>

      <section className="admin-management" aria-labelledby="admin-management-title">
        <div className="admin-section-heading">
          <Store size={19} aria-hidden="true" />
          <h2 id="admin-management-title">시장·점포 관리 권한</h2>
        </div>
        <div className="admin-management-card">
          <div>
            <span className="admin-permission-badge">관리자 전용</span>
            <h3>시장과 점포 등록·수정 보호 적용</h3>
            <p>
              목록과 상세 조회는 모두에게 공개되지만, 시장과 점포의 등록·수정 요청은
              로그인한 관리자만 처리할 수 있습니다. 시장 상세 화면에서 점포를 관리하세요.
            </p>
          </div>
          <Link to="/">
            관리할 시장 찾아보기
            <ArrowRight size={17} aria-hidden="true" />
          </Link>
        </div>
        <p className="admin-next-note">
          시장 선택 → 상세 화면 → 점포 등록 또는 점포 상세 → 정보 수정 순서로 이용할 수 있습니다.
        </p>
      </section>
    </main>
  )
}
