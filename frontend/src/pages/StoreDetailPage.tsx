import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  Clock3,
  Edit3,
  MapPin,
  Phone,
  RefreshCw,
  SearchX,
  ShoppingBasket,
  Store,
} from 'lucide-react'
import { useEffect } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getStore, StoreApiError } from '../api/storeApi'
import { useAuth } from '../auth/AuthContext'
import { FavoriteButton } from '../components/FavoriteButton'
import { StoreProductSection } from '../components/StoreProductSection'
import { StoreReviewSection } from '../components/StoreReviewSection'
import { STORE_CATEGORY_LABELS, STORE_STATUS_LABELS } from '../constants/store'
import type { StoreDetail } from '../types/store'

interface StoreDetailLocationState {
  message?: string
}

export function StoreDetailPage() {
  const { marketId: marketIdParam, storeId: storeIdParam } = useParams()
  const marketId = Number(marketIdParam)
  const storeId = Number(storeIdParam)
  const isValidIds = Number.isInteger(marketId) && marketId > 0
    && Number.isInteger(storeId) && storeId > 0

  const storeQuery = useQuery({
    queryKey: ['store', marketId, storeId],
    queryFn: () => getStore(marketId, storeId),
    enabled: isValidIds,
  })

  if (!isValidIds) return <StoreDetailState kind="not-found" marketId={marketId} />
  if (storeQuery.isLoading) return <StoreDetailSkeleton />

  if (storeQuery.isError || !storeQuery.data) {
    const isNotFound = storeQuery.error instanceof StoreApiError
      && storeQuery.error.status === 404

    return (
      <StoreDetailState
        kind={isNotFound ? 'not-found' : 'error'}
        marketId={marketId}
        onRetry={isNotFound ? undefined : () => storeQuery.refetch()}
      />
    )
  }

  return <StoreDetailContent store={storeQuery.data} />
}

function StoreDetailContent({ store }: { store: StoreDetail }) {
  const { user } = useAuth()
  const location = useLocation()
  const locationState = location.state as StoreDetailLocationState | null

  useEffect(() => {
    document.title = `${store.name} | 우리동네 전통시장`
  }, [store.name])

  return (
    <main className="store-detail-main">
      <nav className="detail-breadcrumb" aria-label="현재 위치">
        <Link to={`/markets/${store.marketId}`}>
          <ArrowLeft size={17} aria-hidden="true" />
          {store.marketName}
        </Link>
        <span aria-hidden="true">/</span>
        <span>{store.name}</span>
      </nav>

      {locationState?.message && (
        <div className="store-save-success" role="status">{locationState.message}</div>
      )}

      <section className="store-detail-hero" aria-labelledby="store-detail-title">
        <div className="store-detail-visual">
          {store.imageUrl ? (
            <img src={store.imageUrl} alt={`${store.name} 점포`} />
          ) : (
            <div className="store-detail-placeholder" aria-label="점포 이미지 준비 중">
              <ShoppingBasket size={58} strokeWidth={1.4} aria-hidden="true" />
              <span>LOCAL STORE</span>
            </div>
          )}
          <span className={`store-status store-status-${store.status.toLowerCase()}`}>
            {STORE_STATUS_LABELS[store.status]}
          </span>
        </div>

        <div className="store-detail-summary">
          <div className="store-detail-labels">
            <span className="store-category">{STORE_CATEGORY_LABELS[store.category]}</span>
            <span>점포 #{String(store.id).padStart(2, '0')}</span>
          </div>
          <h1 id="store-detail-title">{store.name}</h1>
          <Link className="store-market-link" to={`/markets/${store.marketId}`}>
            <Store size={17} aria-hidden="true" />
            {store.marketName}
          </Link>
          <p className="store-detail-description">
            {store.description || '점포에 대한 자세한 소개가 곧 등록될 예정입니다.'}
          </p>
          <div className="store-detail-actions">
            <FavoriteButton storeId={store.id} />
            {store.phone && (
              <a className="primary-action" href={`tel:${store.phone}`}>
                <Phone size={18} aria-hidden="true" />
                점포에 전화하기
              </a>
            )}
            {user?.role === 'ADMIN' && (
              <Link
                className="store-edit-action"
                to={`/admin/markets/${store.marketId}/stores/${store.id}/edit`}
              >
                <Edit3 size={17} aria-hidden="true" />
                점포 정보 수정
              </Link>
            )}
          </div>
        </div>
      </section>

      <section className="store-detail-info" aria-labelledby="store-info-title">
        <div className="detail-section-heading">
          <span className="section-kicker">Store information</span>
          <h2 id="store-info-title">이용 안내</h2>
        </div>
        <div className="detail-info-grid">
          <StoreInfoCard icon={<MapPin size={22} />} label="점포 위치" value={store.location || '위치 확인 필요'} />
          <StoreInfoCard icon={<Clock3 size={22} />} label="운영시간" value={store.openingHours || '운영시간 확인 필요'} />
          <StoreInfoCard icon={<Phone size={22} />} label="문의 전화" value={store.phone || '전화번호 확인 필요'} href={store.phone ? `tel:${store.phone}` : undefined} />
          <StoreInfoCard icon={<Store size={22} />} label="정기 휴무" value={store.closedDays || '휴무일 확인 필요'} />
        </div>
        <p className="store-updated-at">최종 수정 {formatDate(store.updatedAt)}</p>
      </section>

      <StoreProductSection
        marketId={store.marketId}
        storeId={store.id}
        storeName={store.name}
      />

      <StoreReviewSection storeId={store.id} />
    </main>
  )
}

function StoreInfoCard({
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
      <span className="info-card-icon" aria-hidden="true">{icon}</span>
      <div>
        <span>{label}</span>
        {href ? <a href={href}>{value}</a> : <strong>{value}</strong>}
      </div>
    </article>
  )
}

function StoreDetailState({
  kind,
  marketId,
  onRetry,
}: {
  kind: 'error' | 'not-found'
  marketId: number
  onRetry?: () => void
}) {
  const isNotFound = kind === 'not-found'
  const marketPath = Number.isInteger(marketId) && marketId > 0 ? `/markets/${marketId}` : '/'

  return (
    <main className="detail-state-wrap">
      <div className="state-card detail-state" role={isNotFound ? undefined : 'alert'}>
        <span className={`state-icon ${isNotFound ? '' : 'error-icon'}`}>
          {isNotFound
            ? <SearchX size={27} aria-hidden="true" />
            : <RefreshCw size={25} aria-hidden="true" />}
        </span>
        <h1>{isNotFound ? '찾을 수 없는 점포예요' : '점포 정보를 불러오지 못했어요'}</h1>
        <p>{isNotFound ? '점포 주소를 확인하거나 시장 상세 화면에서 다시 선택해주세요.' : '서버 상태를 확인한 뒤 다시 시도해주세요.'}</p>
        <div className="detail-state-actions">
          {onRetry && <button type="button" onClick={onRetry}>다시 불러오기</button>}
          <Link to={marketPath}>시장으로 돌아가기</Link>
        </div>
      </div>
    </main>
  )
}

function StoreDetailSkeleton() {
  return (
    <main className="store-detail-main" aria-label="점포 상세 정보를 불러오는 중" aria-busy="true">
      <div className="skeleton detail-breadcrumb-skeleton" />
      <section className="store-detail-hero store-detail-skeleton" aria-hidden="true">
        <div className="skeleton store-detail-visual" />
        <div className="store-detail-summary">
          <div className="skeleton detail-kicker-skeleton" />
          <div className="skeleton detail-title-skeleton" />
          <div className="skeleton detail-copy-skeleton" />
          <div className="skeleton detail-copy-skeleton short" />
        </div>
      </section>
    </main>
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
