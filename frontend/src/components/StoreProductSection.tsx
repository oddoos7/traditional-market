import { keepPreviousData, useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  ArrowRight,
  Box,
  PackageOpen,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  SearchX,
  ShoppingBag,
} from 'lucide-react'
import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProducts } from '../api/productApi'
import { useAuth } from '../auth/AuthContext'
import {
  formatProductPrice,
  PRODUCT_SORT_LABELS,
  PRODUCT_STATUS_LABELS,
} from '../constants/product'
import type {
  ProductQueryParams,
  ProductSort,
  ProductStatus,
  ProductSummary,
} from '../types/product'

const INITIAL_FILTERS: ProductQueryParams = {
  keyword: '',
  status: '',
  page: 0,
  size: 6,
  sort: 'LATEST',
}

export function StoreProductSection({
  marketId,
  storeId,
  storeName,
}: {
  marketId: number
  storeId: number
  storeName: string
}) {
  const { user } = useAuth()
  const [keywordInput, setKeywordInput] = useState('')
  const [filters, setFilters] = useState<ProductQueryParams>(INITIAL_FILTERS)
  const productsQuery = useQuery({
    queryKey: ['products', storeId, filters],
    queryFn: () => getProducts(storeId, filters),
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

  const handleReset = () => {
    setKeywordInput('')
    setFilters(INITIAL_FILTERS)
  }

  const handlePageChange = (page: number) => {
    setFilters((current) => ({ ...current, page }))
    document.getElementById('store-products')?.scrollIntoView({ behavior: 'smooth' })
  }

  const hasActiveFilters = Boolean(
    filters.keyword || filters.status || filters.sort !== 'LATEST',
  )
  const data = productsQuery.data

  return (
    <section
      className="store-product-section"
      id="store-products"
      aria-labelledby="store-product-title"
    >
      <div className="product-section-heading">
        <div>
          <span className="section-kicker">Store products</span>
          <h2 id="store-product-title">판매 상품</h2>
          <p>{storeName}에서 준비한 상품을 살펴보세요.</p>
        </div>
        {user?.role === 'ADMIN' && (
          <Link
            className="product-add-link"
            to={`/admin/markets/${marketId}/stores/${storeId}/products/new`}
          >
            <Plus size={17} aria-hidden="true" />
            상품 등록
          </Link>
        )}
      </div>

      <div className="product-toolbar">
        <form className="product-search" onSubmit={handleSearch} role="search">
          <Search size={18} aria-hidden="true" />
          <label className="sr-only" htmlFor={`product-search-${storeId}`}>상품 검색</label>
          <input
            id={`product-search-${storeId}`}
            type="search"
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            placeholder="상품명이나 설명 검색"
          />
          <button type="submit">검색</button>
        </form>

        <div className="product-filter-controls" aria-label="상품 목록 필터">
          <label>
            <span className="sr-only">판매 상태</span>
            <select
              value={filters.status}
              onChange={(event) => setFilters((current) => ({
                ...current,
                status: event.target.value as Exclude<ProductStatus, 'HIDDEN'> | '',
                page: 0,
              }))}
            >
              <option value="">모든 상품</option>
              <option value="ON_SALE">판매 중</option>
              <option value="SOLD_OUT">품절</option>
            </select>
          </label>
          <label>
            <span className="sr-only">상품 정렬</span>
            <select
              value={filters.sort}
              onChange={(event) => setFilters((current) => ({
                ...current,
                sort: event.target.value as ProductSort,
                page: 0,
              }))}
            >
              {Object.entries(PRODUCT_SORT_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          {hasActiveFilters && (
            <button className="product-reset" type="button" onClick={handleReset}>
              <RotateCcw size={14} aria-hidden="true" />
              초기화
            </button>
          )}
        </div>
      </div>

      {productsQuery.isLoading && <ProductListSkeleton />}

      {productsQuery.isError && (
        <div className="product-inline-state" role="alert">
          <RefreshCw size={22} aria-hidden="true" />
          <div>
            <strong>상품 정보를 불러오지 못했어요</strong>
            <span>서버 상태를 확인한 뒤 다시 시도해주세요.</span>
          </div>
          <button type="button" onClick={() => productsQuery.refetch()}>다시 불러오기</button>
        </div>
      )}

      {data && data.content.length === 0 && (
        <div className="product-empty-state">
          {hasActiveFilters
            ? <SearchX size={29} aria-hidden="true" />
            : <PackageOpen size={29} aria-hidden="true" />}
          <strong>{hasActiveFilters ? '조건에 맞는 상품이 없어요' : '아직 등록된 상품이 없어요'}</strong>
          <p>
            {hasActiveFilters
              ? '검색어나 판매 상태를 바꿔보세요.'
              : '새 상품이 등록되면 이곳에 표시됩니다.'}
          </p>
          {hasActiveFilters && <button type="button" onClick={handleReset}>전체 상품 보기</button>}
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <div className={`product-grid ${productsQuery.isFetching ? 'is-refreshing' : ''}`}>
            {data.content.map((product) => (
              <ProductCard key={product.id} marketId={marketId} product={product} />
            ))}
          </div>
          <div className="product-results-footer">
            <p>총 <strong>{data.totalElements}</strong>개 상품</p>
            <ProductPagination
              page={data.page}
              totalPages={data.totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        </>
      )}
    </section>
  )
}

function ProductCard({ marketId, product }: { marketId: number; product: ProductSummary }) {
  return (
    <article className="product-card">
      <Link to={`/markets/${marketId}/stores/${product.storeId}/products/${product.id}`}>
        <div className="product-card-visual">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={`${product.name} 상품`} />
          ) : (
            <span className="product-card-placeholder" aria-label="상품 이미지 준비 중">
              <ShoppingBag size={36} strokeWidth={1.55} aria-hidden="true" />
              <small>LOCAL GOODS</small>
            </span>
          )}
          <span className={`product-status product-status-${product.status.toLowerCase()}`}>
            {PRODUCT_STATUS_LABELS[product.status]}
          </span>
        </div>
        <div className="product-card-body">
          <h3>{product.name}</h3>
          <p>{product.description || '상품 설명이 곧 등록될 예정입니다.'}</p>
          <div className="product-card-price-row">
            <strong>{formatProductPrice(product.price)}</strong>
            <span>
              <Box size={13} aria-hidden="true" />
              {product.stockQuantity > 0 ? `재고 ${product.stockQuantity.toLocaleString()}개` : '재고 없음'}
            </span>
          </div>
          <span className="product-card-more">
            상품 자세히 보기 <ArrowRight size={15} aria-hidden="true" />
          </span>
        </div>
      </Link>
    </article>
  )
}

function ProductPagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  return (
    <nav className="product-pagination" aria-label="상품 목록 페이지">
      <button
        type="button"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label="이전 상품 페이지"
      >
        <ArrowLeft size={16} aria-hidden="true" />
      </button>
      <span><strong>{page + 1}</strong> / {totalPages}</span>
      <button
        type="button"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="다음 상품 페이지"
      >
        <ArrowRight size={16} aria-hidden="true" />
      </button>
    </nav>
  )
}

function ProductListSkeleton() {
  return (
    <div className="product-grid" aria-label="상품 목록을 불러오는 중" aria-busy="true">
      {Array.from({ length: 3 }, (_, index) => (
        <div className="product-card product-card-skeleton" key={index} aria-hidden="true">
          <div className="skeleton product-card-visual" />
          <div className="product-card-body">
            <div className="skeleton product-skeleton-title" />
            <div className="skeleton product-skeleton-line" />
            <div className="skeleton product-skeleton-price" />
          </div>
        </div>
      ))}
    </div>
  )
}
