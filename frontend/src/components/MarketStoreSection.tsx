import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Clock3, MapPin, Plus, RefreshCw, ShoppingBasket } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getStores } from '../api/storeApi'
import { useAuth } from '../auth/AuthContext'
import { STORE_CATEGORY_LABELS, STORE_STATUS_LABELS } from '../constants/store'
import type { StoreSummary } from '../types/store'

export function MarketStoreSection({
  marketId,
  marketName,
}: {
  marketId: number
  marketName: string
}) {
  const { user } = useAuth()
  const storesQuery = useQuery({
    queryKey: ['stores', marketId, { page: 0, size: 6, sort: 'LATEST' }],
    queryFn: () => getStores(marketId, { page: 0, size: 6, sort: 'LATEST' }),
  })

  return (
    <section className="market-store-section" aria-labelledby="market-store-title">
      <div className="store-section-heading">
        <div>
          <span className="section-kicker">Market stores</span>
          <h2 id="market-store-title">입점 점포</h2>
          <p>{marketName}에서 만날 수 있는 점포를 확인해보세요.</p>
        </div>
        {user?.role === 'ADMIN' && (
          <Link className="store-add-link" to={`/admin/markets/${marketId}/stores/new`}>
            <Plus size={17} aria-hidden="true" />
            점포 등록
          </Link>
        )}
      </div>

      {storesQuery.isLoading && (
        <div className="store-preview-grid" aria-label="점포 목록을 불러오는 중" aria-busy="true">
          {Array.from({ length: 3 }, (_, index) => (
            <div className="store-preview-card store-card-skeleton" key={index} aria-hidden="true">
              <div className="skeleton store-preview-visual" />
              <div className="skeleton store-preview-title" />
              <div className="skeleton store-preview-line" />
            </div>
          ))}
        </div>
      )}

      {storesQuery.isError && (
        <div className="store-inline-state" role="alert">
          <RefreshCw size={20} aria-hidden="true" />
          <div>
            <strong>점포 정보를 불러오지 못했어요</strong>
            <span>잠시 후 다시 시도해주세요.</span>
          </div>
          <button type="button" onClick={() => storesQuery.refetch()}>다시 불러오기</button>
        </div>
      )}

      {storesQuery.data?.content.length === 0 && (
        <div className="store-empty-state">
          <ShoppingBasket size={27} aria-hidden="true" />
          <strong>아직 등록된 점포가 없습니다</strong>
          <p>시장의 첫 점포 정보가 등록되면 이곳에 표시됩니다.</p>
        </div>
      )}

      {storesQuery.data && storesQuery.data.content.length > 0 && (
        <>
          <div className="store-preview-grid">
            {storesQuery.data.content.map((store) => (
              <StorePreviewCard key={store.id} store={store} />
            ))}
          </div>
          <p className="store-total-count">
            총 <strong>{storesQuery.data.totalElements}</strong>개 점포
          </p>
        </>
      )}
    </section>
  )
}

function StorePreviewCard({ store }: { store: StoreSummary }) {
  return (
    <article className="store-preview-card">
      <Link to={`/markets/${store.marketId}/stores/${store.id}`}>
        <div className="store-preview-visual">
          {store.imageUrl ? (
            <img src={store.imageUrl} alt={`${store.name} 점포`} />
          ) : (
            <span className="store-preview-placeholder" aria-label="점포 이미지 준비 중">
              <ShoppingBasket size={32} strokeWidth={1.7} aria-hidden="true" />
            </span>
          )}
          <span className={`store-status store-status-${store.status.toLowerCase()}`}>
            {STORE_STATUS_LABELS[store.status]}
          </span>
        </div>
        <div className="store-preview-body">
          <span className="store-category">{STORE_CATEGORY_LABELS[store.category]}</span>
          <h3>{store.name}</h3>
          <p>{store.description || '점포 소개가 곧 등록될 예정입니다.'}</p>
          <div className="store-preview-meta">
            <span><MapPin size={14} aria-hidden="true" />{store.location || '위치 확인 필요'}</span>
            <span><Clock3 size={14} aria-hidden="true" />{store.openingHours || '운영시간 확인 필요'}</span>
          </div>
          <span className="store-preview-more">
            점포 상세 보기 <ArrowRight size={15} aria-hidden="true" />
          </span>
        </div>
      </Link>
    </article>
  )
}
