import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowRight,
  Clock3,
  Heart,
  LoaderCircle,
  MapPin,
  RefreshCw,
  ShoppingBasket,
  Trash2,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  FavoriteApiError,
  getFavoriteStores,
  removeFavorite,
} from '../api/favoriteApi'
import { useAuth } from '../auth/AuthContext'
import { STORE_CATEGORY_LABELS, STORE_STATUS_LABELS } from '../constants/store'
import type { FavoriteStore } from '../types/favorite'

export function FavoritesPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [errorMessage, setErrorMessage] = useState('')
  const favoritesQuery = useQuery({
    queryKey: ['favorites', user?.id],
    queryFn: getFavoriteStores,
    enabled: Boolean(user),
    retry: false,
  })

  const removeMutation = useMutation({
    mutationFn: removeFavorite,
    onSuccess: (_, storeId) => {
      queryClient.setQueryData(
        ['favorites', user?.id],
        (favorites: FavoriteStore[] | undefined) => (
          favorites?.filter((favorite) => favorite.storeId !== storeId) ?? []
        ),
      )
      queryClient.setQueryData(
        ['favorite-status', user?.id, storeId],
        { favorite: false },
      )
    },
    onError: (error) => {
      setErrorMessage(
        error instanceof FavoriteApiError
          ? error.message
          : '찜을 해제하지 못했습니다. 다시 시도해주세요.',
      )
    },
  })

  useEffect(() => {
    document.title = '찜한 점포 | 우리동네 전통시장'
  }, [])

  return (
    <main className="favorites-main">
      <section className="favorites-heading" aria-labelledby="favorites-title">
        <div className="favorites-heading-icon" aria-hidden="true">
          <Heart size={25} fill="currentColor" />
        </div>
        <div>
          <span className="eyebrow">My favorite stores</span>
          <h1 id="favorites-title">찜한 점포</h1>
          <p>다시 방문하고 싶은 우리 동네 점포를 한곳에서 확인하세요.</p>
        </div>
      </section>

      {favoritesQuery.isLoading && (
        <div className="favorites-loading" aria-label="찜한 점포를 불러오는 중">
          <LoaderCircle className="spin" size={27} aria-hidden="true" />
          <span>찜한 점포를 불러오고 있어요.</span>
        </div>
      )}

      {favoritesQuery.isError && (
        <div className="favorites-state" role="alert">
          <RefreshCw size={27} aria-hidden="true" />
          <h2>찜 목록을 불러오지 못했어요</h2>
          <p>잠시 후 다시 시도해주세요.</p>
          <button type="button" onClick={() => favoritesQuery.refetch()}>다시 불러오기</button>
        </div>
      )}

      {favoritesQuery.data?.length === 0 && (
        <div className="favorites-state">
          <Heart size={30} aria-hidden="true" />
          <h2>아직 찜한 점포가 없어요</h2>
          <p>시장과 점포를 둘러보고 마음에 드는 곳을 저장해보세요.</p>
          <Link to="/">시장 둘러보기</Link>
        </div>
      )}

      {favoritesQuery.data && favoritesQuery.data.length > 0 && (
        <section aria-label="찜한 점포 목록">
          <div className="favorites-summary">
            <p><strong>{user?.name}</strong>님이 저장한 점포</p>
            <span>총 {favoritesQuery.data.length}곳</span>
          </div>
          {errorMessage && <p className="favorites-error" role="alert">{errorMessage}</p>}
          <div className="favorite-grid">
            {favoritesQuery.data.map((favorite) => (
              <FavoriteStoreCard
                key={favorite.favoriteId}
                favorite={favorite}
                isRemoving={removeMutation.isPending
                  && removeMutation.variables === favorite.storeId}
                onRemove={() => {
                  setErrorMessage('')
                  removeMutation.mutate(favorite.storeId)
                }}
              />
            ))}
          </div>
        </section>
      )}
    </main>
  )
}

function FavoriteStoreCard({
  favorite,
  isRemoving,
  onRemove,
}: {
  favorite: FavoriteStore
  isRemoving: boolean
  onRemove: () => void
}) {
  const storePath = `/markets/${favorite.marketId}/stores/${favorite.storeId}`

  return (
    <article className="favorite-card">
      <Link className="favorite-card-visual" to={storePath}>
        {favorite.imageUrl ? (
          <img src={favorite.imageUrl} alt={`${favorite.name} 점포`} />
        ) : (
          <span className="store-preview-placeholder" aria-label="점포 이미지 준비 중">
            <ShoppingBasket size={36} strokeWidth={1.6} aria-hidden="true" />
          </span>
        )}
        <span className={`store-status store-status-${favorite.status.toLowerCase()}`}>
          {STORE_STATUS_LABELS[favorite.status]}
        </span>
      </Link>
      <div className="favorite-card-body">
        <div className="favorite-card-topline">
          <span className="store-category">{STORE_CATEGORY_LABELS[favorite.category]}</span>
          <button
            type="button"
            onClick={onRemove}
            disabled={isRemoving}
            aria-label={`${favorite.name} 찜 해제`}
          >
            {isRemoving
              ? <LoaderCircle className="spin" size={17} aria-hidden="true" />
              : <Trash2 size={17} aria-hidden="true" />}
            {isRemoving ? '해제 중' : '찜 해제'}
          </button>
        </div>
        <Link className="favorite-card-title" to={storePath}>{favorite.name}</Link>
        <Link className="favorite-market-name" to={`/markets/${favorite.marketId}`}>
          {favorite.marketName}
        </Link>
        <p>{favorite.description || '점포 소개가 곧 등록될 예정입니다.'}</p>
        <div className="store-preview-meta">
          <span><MapPin size={14} aria-hidden="true" />{favorite.location || '위치 확인 필요'}</span>
          <span><Clock3 size={14} aria-hidden="true" />{favorite.openingHours || '운영시간 확인 필요'}</span>
        </div>
        <Link className="store-preview-more" to={storePath}>
          점포 상세 보기 <ArrowRight size={15} aria-hidden="true" />
        </Link>
      </div>
    </article>
  )
}
