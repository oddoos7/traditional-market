import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  Box,
  CalendarDays,
  Edit3,
  PackageCheck,
  RefreshCw,
  SearchX,
  ShoppingBag,
  Store,
} from 'lucide-react'
import { useEffect } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getProduct, ProductApiError } from '../api/productApi'
import { useAuth } from '../auth/AuthContext'
import { formatProductPrice, PRODUCT_STATUS_LABELS } from '../constants/product'
import type { ProductDetail } from '../types/product'

interface ProductDetailLocationState {
  message?: string
}

export function ProductDetailPage() {
  const {
    marketId: marketIdParam,
    storeId: storeIdParam,
    productId: productIdParam,
  } = useParams()
  const marketId = Number(marketIdParam)
  const storeId = Number(storeIdParam)
  const productId = Number(productIdParam)
  const isValidIds = [marketId, storeId, productId]
    .every((id) => Number.isInteger(id) && id > 0)

  const productQuery = useQuery({
    queryKey: ['product', storeId, productId],
    queryFn: () => getProduct(storeId, productId),
    enabled: isValidIds,
  })

  if (!isValidIds) return <ProductDetailState kind="not-found" />
  if (productQuery.isLoading) return <ProductDetailSkeleton />

  if (productQuery.isError || !productQuery.data) {
    const isNotFound = productQuery.error instanceof ProductApiError
      && productQuery.error.status === 404

    return (
      <ProductDetailState
        kind={isNotFound ? 'not-found' : 'error'}
        storePath={`/markets/${marketId}/stores/${storeId}`}
        onRetry={isNotFound ? undefined : () => productQuery.refetch()}
      />
    )
  }

  if (productQuery.data.marketId !== marketId) {
    return (
      <ProductDetailState
        kind="not-found"
        storePath={`/markets/${productQuery.data.marketId}/stores/${storeId}`}
      />
    )
  }

  return <ProductDetailContent product={productQuery.data} />
}

function ProductDetailContent({ product }: { product: ProductDetail }) {
  const { user } = useAuth()
  const location = useLocation()
  const locationState = location.state as ProductDetailLocationState | null
  const storePath = `/markets/${product.marketId}/stores/${product.storeId}`

  useEffect(() => {
    document.title = `${product.name} | 우리동네 전통시장`
  }, [product.name])

  return (
    <main className="product-detail-main">
      <nav className="detail-breadcrumb" aria-label="현재 위치">
        <Link to={storePath}>
          <ArrowLeft size={17} aria-hidden="true" />
          {product.storeName}
        </Link>
        <span aria-hidden="true">/</span>
        <span>{product.name}</span>
      </nav>

      {locationState?.message && (
        <div className="store-save-success" role="status">{locationState.message}</div>
      )}

      <section className="product-detail-hero" aria-labelledby="product-detail-title">
        <div className="product-detail-visual">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={`${product.name} 상품`} />
          ) : (
            <div className="product-detail-placeholder" aria-label="상품 이미지 준비 중">
              <ShoppingBag size={64} strokeWidth={1.35} aria-hidden="true" />
              <span>LOCAL GOODS</span>
            </div>
          )}
          <span className={`product-status product-status-${product.status.toLowerCase()}`}>
            {PRODUCT_STATUS_LABELS[product.status]}
          </span>
        </div>

        <div className="product-detail-summary">
          <span className="eyebrow">Product #{String(product.id).padStart(3, '0')}</span>
          <h1 id="product-detail-title">{product.name}</h1>
          <Link className="product-store-link" to={storePath}>
            <Store size={17} aria-hidden="true" />
            {product.marketName} · {product.storeName}
          </Link>
          <strong className="product-detail-price">{formatProductPrice(product.price)}</strong>
          <p className="product-detail-description">
            {product.description || '상품에 대한 자세한 설명이 곧 등록될 예정입니다.'}
          </p>

          <div className="product-stock-panel">
            <span aria-hidden="true">
              {product.status === 'ON_SALE'
                ? <PackageCheck size={22} />
                : <Box size={22} />}
            </span>
            <div>
              <small>현재 재고</small>
              <strong>
                {product.stockQuantity > 0
                  ? `${product.stockQuantity.toLocaleString()}개`
                  : '재고 없음'}
              </strong>
            </div>
            <p>
              {product.status === 'ON_SALE'
                ? '구매 가능한 상품입니다.'
                : '현재는 구매할 수 없는 상품입니다.'}
            </p>
          </div>

          <div className="product-detail-actions">
            <Link className="secondary-action" to={storePath}>
              <ArrowLeft size={18} aria-hidden="true" />
              점포로 돌아가기
            </Link>
            {user?.role === 'ADMIN' && (
              <Link
                className="store-edit-action"
                to={`/admin/markets/${product.marketId}/stores/${product.storeId}/products/${product.id}/edit`}
              >
                <Edit3 size={17} aria-hidden="true" />
                상품 정보 수정
              </Link>
            )}
          </div>
        </div>
      </section>

      <section className="product-detail-meta" aria-label="상품 등록 정보">
        <div>
          <CalendarDays size={19} aria-hidden="true" />
          <span>등록일</span>
          <strong>{formatDate(product.createdAt)}</strong>
        </div>
        <div>
          <CalendarDays size={19} aria-hidden="true" />
          <span>최종 수정</span>
          <strong>{formatDate(product.updatedAt)}</strong>
        </div>
      </section>
    </main>
  )
}

function ProductDetailState({
  kind,
  storePath = '/',
  onRetry,
}: {
  kind: 'error' | 'not-found'
  storePath?: string
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
        <h1>{isNotFound ? '찾을 수 없는 상품이에요' : '상품 정보를 불러오지 못했어요'}</h1>
        <p>
          {isNotFound
            ? '상품 주소를 확인하거나 점포 화면에서 다시 선택해주세요.'
            : '서버 상태를 확인한 뒤 다시 시도해주세요.'}
        </p>
        <div className="detail-state-actions">
          {onRetry && <button type="button" onClick={onRetry}>다시 불러오기</button>}
          <Link to={storePath}>점포로 돌아가기</Link>
        </div>
      </div>
    </main>
  )
}

function ProductDetailSkeleton() {
  return (
    <main className="product-detail-main" aria-label="상품 상세 정보를 불러오는 중" aria-busy="true">
      <div className="skeleton detail-breadcrumb-skeleton" />
      <section className="product-detail-hero product-detail-skeleton" aria-hidden="true">
        <div className="skeleton product-detail-visual" />
        <div className="product-detail-summary">
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
