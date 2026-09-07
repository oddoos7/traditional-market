import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Clock3,
  Info,
  Heart,
  LoaderCircle,
  LogIn,
  LogOut,
  MapPin,
  Phone,
  RefreshCw,
  RotateCcw,
  Search,
  SearchX,
  ShieldCheck,
  SlidersHorizontal,
  Store,
  UserPlus,
} from 'lucide-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Dispatch, FormEvent, ReactNode, SetStateAction, useEffect, useState } from 'react'
import {
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useParams,
} from 'react-router-dom'
import { getMarket, getMarkets, MarketApiError } from './api/marketApi'
import { useAuth } from './auth/AuthContext'
import { AdminRoute } from './auth/AdminRoute'
import { RequireAuthRoute } from './auth/RequireAuthRoute'
import { AdminPage } from './pages/AdminPage'
import { MarketStoreSection } from './components/MarketStoreSection'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { StoreDetailPage } from './pages/StoreDetailPage'
import { StoreFormPage } from './pages/StoreFormPage'
import { FavoritesPage } from './pages/FavoritesPage'
import type {
  Market,
  MarketDetail,
  MarketQueryParams,
  MarketSort,
  MarketStatus,
} from './types/market'
import './App.css'

const INITIAL_FILTERS: MarketQueryParams = {
  keyword: '',
  status: '',
  page: 0,
  size: 6,
  sort: 'LATEST',
}

const STATUS_LABELS: Record<MarketStatus, string> = {
  OPEN: '운영 중',
  TEMPORARILY_CLOSED: '임시 휴무',
  CLOSED: '운영 종료',
}

function App() {
  const [keywordInput, setKeywordInput] = useState('')
  const [filters, setFilters] = useState<MarketQueryParams>(INITIAL_FILTERS)

  return (
    <Routes>
      <Route element={<SiteLayout />}>
        <Route
          index
          element={(
            <MarketListPage
              keywordInput={keywordInput}
              setKeywordInput={setKeywordInput}
              filters={filters}
              setFilters={setFilters}
            />
          )}
        />
        <Route path="markets/:marketId" element={<MarketDetailPage />} />
        <Route path="markets/:marketId/stores/:storeId" element={<StoreDetailPage />} />
        <Route
          path="favorites"
          element={<RequireAuthRoute><FavoritesPage /></RequireAuthRoute>}
        />
        <Route
          path="login"
          element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>}
        />
        <Route
          path="signup"
          element={<PublicOnlyRoute><SignupPage /></PublicOnlyRoute>}
        />
        <Route
          path="admin"
          element={<AdminRoute><AdminPage /></AdminRoute>}
        />
        <Route
          path="admin/markets/:marketId/stores/new"
          element={<AdminRoute><StoreFormPage /></AdminRoute>}
        />
        <Route
          path="admin/markets/:marketId/stores/:storeId/edit"
          element={<AdminRoute><StoreFormPage /></AdminRoute>}
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

function SiteLayout() {
  const { pathname } = useLocation()
  const { user, isCheckingAuth, isLoggingOut, logoutUser } = useAuth()
  const [logoutError, setLogoutError] = useState('')

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' })
    setLogoutError('')
    if (pathname === '/') document.title = '우리동네 전통시장'
  }, [pathname])

  const handleLogout = async () => {
    setLogoutError('')

    try {
      await logoutUser()
    } catch {
      setLogoutError('로그아웃하지 못했습니다. 다시 시도해주세요.')
    }
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/" aria-label="우리동네 전통시장 홈">
            <span className="brand-mark" aria-hidden="true">
              <Store size={22} strokeWidth={2.2} />
            </span>
            <span>
              <strong>우리동네</strong>
              <small>전통시장</small>
            </span>
          </Link>
          <div className="header-actions">
            {isCheckingAuth ? (
              <span className="header-auth-loading" aria-label="로그인 상태 확인 중">
                <LoaderCircle className="spin" size={18} aria-hidden="true" />
              </span>
            ) : user ? (
              <>
                <Link className="header-favorites" to="/favorites">
                  <Heart size={16} aria-hidden="true" />
                  <span>찜한 점포</span>
                </Link>
                {user.role === 'ADMIN' && (
                  <Link className="header-admin" to="/admin">
                    <ShieldCheck size={16} aria-hidden="true" />
                    <span>관리자 센터</span>
                  </Link>
                )}
                <div className="header-user">
                  <span className="header-avatar" aria-hidden="true">
                    {user.name.slice(0, 1).toUpperCase()}
                  </span>
                  <span>
                    <strong>{user.name}</strong>
                    <small>{user.role === 'ADMIN' ? '관리자' : '일반 회원'}</small>
                  </span>
                </div>
                <button
                  className="header-logout"
                  type="button"
                  onClick={handleLogout}
                  disabled={isLoggingOut}
                >
                  {isLoggingOut
                    ? <LoaderCircle className="spin" size={16} aria-hidden="true" />
                    : <LogOut size={16} aria-hidden="true" />}
                  <span>{isLoggingOut ? '처리 중' : '로그아웃'}</span>
                </button>
              </>
            ) : (
              <>
                <Link className="header-login" to="/login">
                  <LogIn size={16} aria-hidden="true" />
                  로그인
                </Link>
                <Link className="header-signup" to="/signup">
                  <UserPlus size={16} aria-hidden="true" />
                  회원가입
                </Link>
              </>
            )}
          </div>
        </div>
        {logoutError && <p className="header-error" role="alert">{logoutError}</p>}
      </header>

      <Outlet />

      <footer className="site-footer">
        <p>시장과 사람을 더 가깝게, 우리동네 전통시장</p>
      </footer>
    </div>
  )
}

function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { user, isCheckingAuth } = useAuth()

  if (isCheckingAuth) {
    return (
      <main className="auth-route-loading" aria-label="로그인 상태를 확인하는 중">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
      </main>
    )
  }

  return user ? <Navigate to="/" replace /> : children
}

interface MarketListPageProps {
  keywordInput: string
  setKeywordInput: Dispatch<SetStateAction<string>>
  filters: MarketQueryParams
  setFilters: Dispatch<SetStateAction<MarketQueryParams>>
}

function MarketListPage({
  keywordInput,
  setKeywordInput,
  filters,
  setFilters,
}: MarketListPageProps) {
  const marketsQuery = useQuery({
    queryKey: ['markets', filters],
    queryFn: () => getMarkets(filters),
    placeholderData: keepPreviousData,
  })

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFilters((current) => ({
      ...current,
      keyword: keywordInput.trim(),
      page: 0,
    }))
  }

  const handleStatusChange = (status: MarketStatus | '') => {
    setFilters((current) => ({ ...current, status, page: 0 }))
  }

  const handleSortChange = (sort: MarketSort) => {
    setFilters((current) => ({ ...current, sort, page: 0 }))
  }

  const handleReset = () => {
    setKeywordInput('')
    setFilters(INITIAL_FILTERS)
  }

  const handlePageChange = (page: number) => {
    setFilters((current) => ({ ...current, page }))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const data = marketsQuery.data
  const hasActiveFilters = Boolean(filters.keyword || filters.status || filters.sort !== 'LATEST')

  return (
    <main>
      <section className="market-intro" aria-labelledby="market-page-title">
        <div className="intro-copy">
          <span className="eyebrow">Market discovery</span>
          <h1 id="market-page-title">오늘, 어떤 시장을 만나볼까요?</h1>
          <p>가까운 시장의 운영 정보와 특별한 이야기를 한눈에 살펴보세요.</p>
        </div>

        <form className="search-panel" onSubmit={handleSearch}>
          <label className="search-field">
            <Search size={21} aria-hidden="true" />
            <span className="sr-only">시장명 또는 주소 검색</span>
            <input
              type="search"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              placeholder="시장명이나 주소를 검색해보세요"
            />
          </label>
          <button className="search-button" type="submit">시장 찾기</button>
        </form>

        <div className="filter-row" aria-label="시장 목록 필터">
          <div className="filter-label">
            <SlidersHorizontal size={17} aria-hidden="true" />
            <span>조건 설정</span>
          </div>
          <label className="select-control">
            <span className="sr-only">운영 상태</span>
            <select
              value={filters.status}
              onChange={(event) => handleStatusChange(event.target.value as MarketStatus | '')}
            >
              <option value="">모든 운영 상태</option>
              <option value="OPEN">운영 중</option>
              <option value="TEMPORARILY_CLOSED">임시 휴무</option>
              <option value="CLOSED">운영 종료</option>
            </select>
          </label>
          <label className="select-control">
            <span className="sr-only">정렬 기준</span>
            <select
              value={filters.sort}
              onChange={(event) => handleSortChange(event.target.value as MarketSort)}
            >
              <option value="LATEST">최근 등록순</option>
              <option value="NAME">이름순</option>
            </select>
          </label>
          {hasActiveFilters && (
            <button className="reset-button" type="button" onClick={handleReset}>
              <RotateCcw size={15} aria-hidden="true" />
              초기화
            </button>
          )}
        </div>
      </section>

      <section className="market-results" aria-labelledby="market-results-title">
        <div className="results-heading">
          <div>
            <span className="section-kicker">동네 시장 둘러보기</span>
            <h2 id="market-results-title">
              {filters.keyword ? `‘${filters.keyword}’ 검색 결과` : '발견한 전통시장'}
            </h2>
          </div>
          {data && (
            <p className="result-count" aria-live="polite">
              총 <strong>{data.totalElements}</strong>곳
            </p>
          )}
        </div>

        {marketsQuery.isLoading && <MarketListSkeleton />}

        {marketsQuery.isError && (
          <div className="state-card" role="alert">
            <span className="state-icon error-icon">
              <RefreshCw size={25} aria-hidden="true" />
            </span>
            <h3>시장 정보를 불러오지 못했어요</h3>
            <p>백엔드 서버가 실행 중인지 확인한 뒤 다시 시도해주세요.</p>
            <button type="button" onClick={() => marketsQuery.refetch()}>다시 불러오기</button>
          </div>
        )}

        {data && data.content.length === 0 && (
          <div className="state-card">
            <span className="state-icon">
              <SearchX size={27} aria-hidden="true" />
            </span>
            <h3>조건에 맞는 시장이 없어요</h3>
            <p>검색어나 운영 상태를 바꾸면 더 많은 시장을 발견할 수 있어요.</p>
            <button type="button" onClick={handleReset}>전체 시장 보기</button>
          </div>
        )}

        {data && data.content.length > 0 && (
          <>
            <div className={`market-grid ${marketsQuery.isFetching ? 'is-refreshing' : ''}`}>
              {data.content.map((market) => (
                <MarketCard key={market.id} market={market} />
              ))}
            </div>

            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              onPageChange={handlePageChange}
            />
          </>
        )}
      </section>
    </main>
  )
}

function MarketCard({ market }: { market: Market }) {
  return (
    <article className="market-card">
      <Link
        className="market-card-link"
        to={`/markets/${market.id}`}
        aria-label={`${market.name} 상세정보 보기`}
      >
        <div className="market-visual">
          {market.imageUrl ? (
            <img src={market.imageUrl} alt={`${market.name} 전경`} />
          ) : (
            <div className="market-placeholder" aria-label="대표 이미지 준비 중">
              <Store size={34} strokeWidth={1.6} aria-hidden="true" />
              <span>LOCAL MARKET</span>
            </div>
          )}
          <StatusBadge status={market.status} />
        </div>

        <div className="market-card-body">
          <div className="card-title-row">
            <h3>{market.name}</h3>
            <span className="market-number">#{String(market.id).padStart(2, '0')}</span>
          </div>
          <p className="market-description">
            {market.description || '시장 소개가 곧 업데이트될 예정입니다.'}
          </p>
          <dl className="market-meta">
            <div>
              <dt><MapPin size={16} aria-hidden="true" /> 주소</dt>
              <dd>{market.address}</dd>
            </div>
            <div>
              <dt><Clock3 size={16} aria-hidden="true" /> 운영</dt>
              <dd>{market.openingHours || '운영시간 확인 필요'}</dd>
            </div>
            <div>
              <dt><CalendarDays size={16} aria-hidden="true" /> 휴무</dt>
              <dd>{market.closedDays || '휴무일 확인 필요'}</dd>
            </div>
          </dl>
          <span className="card-link-hint">
            상세 정보 보기
            <ArrowRight size={16} aria-hidden="true" />
          </span>
        </div>
      </Link>
    </article>
  )
}

function MarketDetailPage() {
  const { marketId: marketIdParam } = useParams()
  const marketId = Number(marketIdParam)
  const isValidMarketId = Number.isInteger(marketId) && marketId > 0

  const marketQuery = useQuery({
    queryKey: ['market', marketId],
    queryFn: () => getMarket(marketId),
    enabled: isValidMarketId,
  })

  if (!isValidMarketId) return <DetailState kind="not-found" />
  if (marketQuery.isLoading) return <MarketDetailSkeleton />
  if (marketQuery.isError || !marketQuery.data) {
    const isNotFound = marketQuery.error instanceof MarketApiError
      && marketQuery.error.status === 404

    return (
      <DetailState
        kind={isNotFound ? 'not-found' : 'error'}
        onRetry={isNotFound ? undefined : () => marketQuery.refetch()}
      />
    )
  }

  return <MarketDetailContent market={marketQuery.data} />
}

function MarketDetailContent({ market }: { market: MarketDetail }) {
  const fullAddress = [market.address, market.detailAddress].filter(Boolean).join(' ')
  const hasCoordinates = market.latitude !== null && market.longitude !== null

  useEffect(() => {
    const previousTitle = document.title
    const descriptionMeta = document.querySelector<HTMLMetaElement>('meta[name="description"]')
    const previousDescription = descriptionMeta?.content

    document.title = `${market.name} | 우리동네 전통시장`
    if (descriptionMeta) {
      descriptionMeta.content = market.description || `${market.name}의 운영 및 방문 정보입니다.`
    }

    return () => {
      document.title = previousTitle
      if (descriptionMeta && previousDescription) descriptionMeta.content = previousDescription
    }
  }, [market.description, market.name])

  return (
    <main className="detail-main">
      <nav className="detail-breadcrumb" aria-label="현재 위치">
        <Link to="/">
          <ArrowLeft size={17} aria-hidden="true" />
          시장 목록
        </Link>
        <span aria-hidden="true">/</span>
        <span>{market.name}</span>
      </nav>

      <section className="detail-hero" aria-labelledby="market-detail-title">
        <div className="detail-visual">
          {market.imageUrl ? (
            <img src={market.imageUrl} alt={`${market.name} 전경`} />
          ) : (
            <div className="market-placeholder detail-placeholder" aria-label="대표 이미지 준비 중">
              <Store size={54} strokeWidth={1.45} aria-hidden="true" />
              <span>OUR LOCAL MARKET</span>
            </div>
          )}
          <StatusBadge status={market.status} />
        </div>

        <div className="detail-summary">
          <span className="eyebrow">Local market #{String(market.id).padStart(2, '0')}</span>
          <h1 id="market-detail-title">{market.name}</h1>
          <p className="detail-address">
            <MapPin size={18} aria-hidden="true" />
            {fullAddress}
          </p>
          <p className="detail-description">
            {market.description || '시장에 대한 자세한 소개가 곧 업데이트될 예정입니다.'}
          </p>
          <div className="detail-actions">
            {market.phone && (
              <a className="primary-action" href={`tel:${market.phone}`}>
                <Phone size={18} aria-hidden="true" />
                시장에 전화하기
              </a>
            )}
            <Link className="secondary-action" to="/">
              <ArrowLeft size={18} aria-hidden="true" />
              목록으로 돌아가기
            </Link>
          </div>
        </div>
      </section>

      <section className="detail-content" aria-label="시장 이용 안내">
        <div className="detail-section-heading">
          <span className="section-kicker">방문 전 확인하세요</span>
          <h2>시장 이용 안내</h2>
        </div>

        <div className="detail-info-grid">
          <DetailInfoCard
            icon={<Clock3 size={22} aria-hidden="true" />}
            label="운영시간"
            value={market.openingHours || '운영시간 확인 필요'}
          />
          <DetailInfoCard
            icon={<CalendarDays size={22} aria-hidden="true" />}
            label="정기 휴무"
            value={market.closedDays || '휴무일 확인 필요'}
          />
          <DetailInfoCard
            icon={<Phone size={22} aria-hidden="true" />}
            label="문의 전화"
            value={market.phone || '전화번호 확인 필요'}
            href={market.phone ? `tel:${market.phone}` : undefined}
          />
          <DetailInfoCard
            icon={<MapPin size={22} aria-hidden="true" />}
            label="상세 주소"
            value={fullAddress}
          />
        </div>

        <div className="location-panel">
          <div className="location-copy">
            <span className="location-icon">
              <MapPin size={24} aria-hidden="true" />
            </span>
            <div>
              <span className="location-label">시장 위치</span>
              <h3>{market.name}</h3>
              <p>{fullAddress}</p>
            </div>
          </div>
          <div className="coordinate-box">
            {hasCoordinates ? (
              <>
                <span>위도 {market.latitude}</span>
                <span>경도 {market.longitude}</span>
              </>
            ) : (
              <span>위치 좌표를 준비 중입니다.</span>
            )}
          </div>
        </div>

        <div className="visit-note">
          <Info size={19} aria-hidden="true" />
          <p>
            영업시간과 휴무일은 현장 상황에 따라 달라질 수 있습니다.
            방문 전 시장에 한 번 더 확인해주세요.
          </p>
          <span>최종 수정 {formatDate(market.updatedAt)}</span>
        </div>
      </section>

      <MarketStoreSection marketId={market.id} marketName={market.name} />
    </main>
  )
}

function DetailInfoCard({
  icon,
  label,
  value,
  href,
}: {
  icon: React.ReactNode
  label: string
  value: string
  href?: string
}) {
  return (
    <article className="detail-info-card">
      <span className="info-card-icon">{icon}</span>
      <div>
        <span>{label}</span>
        {href ? <a href={href}>{value}</a> : <strong>{value}</strong>}
      </div>
    </article>
  )
}

function StatusBadge({ status }: { status: MarketStatus }) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <span aria-hidden="true" />
      {STATUS_LABELS[status]}
    </span>
  )
}

function DetailState({
  kind,
  onRetry,
}: {
  kind: 'error' | 'not-found'
  onRetry?: () => void
}) {
  const isNotFound = kind === 'not-found'

  return (
    <main className="detail-state-wrap">
      <div className="state-card detail-state" role={isNotFound ? undefined : 'alert'}>
        <span className={`state-icon ${isNotFound ? '' : 'error-icon'}`}>
          {isNotFound
            ? <SearchX size={27} aria-hidden="true" />
            : <RefreshCw size={25} aria-hidden="true" />}
        </span>
        <h1>{isNotFound ? '찾을 수 없는 시장이에요' : '상세 정보를 불러오지 못했어요'}</h1>
        <p>
          {isNotFound
            ? '주소를 다시 확인하거나 시장 목록에서 원하는 시장을 선택해주세요.'
            : '백엔드 서버 상태를 확인한 뒤 다시 시도해주세요.'}
        </p>
        <div className="detail-state-actions">
          {!isNotFound && onRetry && (
            <button type="button" onClick={onRetry}>다시 불러오기</button>
          )}
          <Link to="/">시장 목록으로</Link>
        </div>
      </div>
    </main>
  )
}

function NotFoundPage() {
  return <DetailState kind="not-found" />
}

function MarketDetailSkeleton() {
  return (
    <main className="detail-main" aria-label="시장 상세 정보를 불러오는 중" aria-busy="true">
      <div className="skeleton detail-breadcrumb-skeleton" />
      <section className="detail-hero detail-skeleton" aria-hidden="true">
        <div className="skeleton detail-visual-skeleton" />
        <div className="detail-summary">
          <div className="skeleton detail-kicker-skeleton" />
          <div className="skeleton detail-title-skeleton" />
          <div className="skeleton detail-line-skeleton" />
          <div className="skeleton detail-copy-skeleton" />
          <div className="skeleton detail-copy-skeleton short" />
        </div>
      </section>
    </main>
  )
}

function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  const visiblePages = Array.from({ length: totalPages }, (_, index) => index)
    .filter((pageNumber) => Math.abs(pageNumber - page) <= 2)

  return (
    <nav className="pagination" aria-label="시장 목록 페이지">
      <button
        type="button"
        className="pagination-arrow"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label="이전 페이지"
      >
        <ArrowLeft size={18} aria-hidden="true" />
      </button>
      <div className="page-numbers">
        {visiblePages.map((pageNumber) => (
          <button
            key={pageNumber}
            type="button"
            className={pageNumber === page ? 'active' : ''}
            onClick={() => onPageChange(pageNumber)}
            aria-current={pageNumber === page ? 'page' : undefined}
          >
            {pageNumber + 1}
          </button>
        ))}
      </div>
      <button
        type="button"
        className="pagination-arrow"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="다음 페이지"
      >
        <ArrowRight size={18} aria-hidden="true" />
      </button>
    </nav>
  )
}

function MarketListSkeleton() {
  return (
    <div className="market-grid" aria-label="시장 목록을 불러오는 중" aria-busy="true">
      {Array.from({ length: 6 }, (_, index) => (
        <div className="market-card skeleton-card" key={index} aria-hidden="true">
          <div className="skeleton skeleton-visual" />
          <div className="market-card-body">
            <div className="skeleton skeleton-title" />
            <div className="skeleton skeleton-line" />
            <div className="skeleton skeleton-line short" />
            <div className="skeleton skeleton-meta" />
          </div>
        </div>
      ))}
    </div>
  )
}

function formatDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '확인 중'

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
}

export default App
