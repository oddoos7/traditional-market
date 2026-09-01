import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Clock3,
  MapPin,
  RefreshCw,
  RotateCcw,
  Search,
  SearchX,
  SlidersHorizontal,
  Store,
} from 'lucide-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { FormEvent, useState } from 'react'
import { getMarkets } from './api/marketApi'
import type {
  Market,
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
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <a className="brand" href="/" aria-label="우리동네 전통시장 홈">
            <span className="brand-mark" aria-hidden="true">
              <Store size={22} strokeWidth={2.2} />
            </span>
            <span>
              <strong>우리동네</strong>
              <small>전통시장</small>
            </span>
          </a>
          <div className="header-note">
            <span className="live-dot" aria-hidden="true" />
            우리 동네의 정겨운 시장을 찾아보세요
          </div>
        </div>
      </header>

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
            <button className="search-button" type="submit">
              시장 찾기
            </button>
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
              <button type="button" onClick={() => marketsQuery.refetch()}>
                다시 불러오기
              </button>
            </div>
          )}

          {data && data.content.length === 0 && (
            <div className="state-card">
              <span className="state-icon">
                <SearchX size={27} aria-hidden="true" />
              </span>
              <h3>조건에 맞는 시장이 없어요</h3>
              <p>검색어나 운영 상태를 바꾸면 더 많은 시장을 발견할 수 있어요.</p>
              <button type="button" onClick={handleReset}>
                전체 시장 보기
              </button>
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

      <footer className="site-footer">
        <p>시장과 사람을 더 가깝게, 우리동네 전통시장</p>
      </footer>
    </div>
  )
}

function MarketCard({ market }: { market: Market }) {
  return (
    <article className="market-card">
      <div className="market-visual">
        {market.imageUrl ? (
          <img src={market.imageUrl} alt={`${market.name} 전경`} />
        ) : (
          <div className="market-placeholder" aria-label="대표 이미지 준비 중">
            <Store size={34} strokeWidth={1.6} aria-hidden="true" />
            <span>LOCAL MARKET</span>
          </div>
        )}
        <span className={`status-badge status-${market.status.toLowerCase()}`}>
          <span aria-hidden="true" />
          {STATUS_LABELS[market.status]}
        </span>
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
      </div>
    </article>
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

export default App
