import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  MessageSquareText,
  RefreshCw,
  Star,
  Trash2,
} from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  createStoreReview,
  deleteStoreReview,
  getMyStoreReview,
  getStoreReviews,
  ReviewApiError,
  updateStoreReview,
} from '../api/reviewApi'
import { useAuth } from '../auth/AuthContext'
import type { ReviewSaveInput, StoreReview } from '../types/review'

const REVIEW_PAGE_SIZE = 5

export function StoreReviewSection({ storeId }: { storeId: number }) {
  const { user } = useAuth()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const reviewsQuery = useQuery({
    queryKey: ['store-reviews', storeId, user?.id, page],
    queryFn: () => getStoreReviews(storeId, page, REVIEW_PAGE_SIZE),
  })

  const myReviewQuery = useQuery({
    queryKey: ['my-store-review', storeId, user?.id],
    queryFn: () => getMyStoreReview(storeId),
    enabled: Boolean(user),
    retry: false,
  })

  useEffect(() => {
    if (!user || myReviewQuery.isLoading) return

    setRating(myReviewQuery.data?.rating ?? 5)
    setContent(myReviewQuery.data?.content ?? '')
  }, [myReviewQuery.data, myReviewQuery.isLoading, user])

  const saveMutation = useMutation({
    mutationFn: (input: ReviewSaveInput) => (
      myReviewQuery.data
        ? updateStoreReview(storeId, myReviewQuery.data.id, input)
        : createStoreReview(storeId, input)
    ),
    onSuccess: (review) => {
      queryClient.setQueryData(['my-store-review', storeId, user?.id], review)
      queryClient.invalidateQueries({ queryKey: ['store-reviews', storeId] })
      setPage(0)
      setSuccessMessage(myReviewQuery.data ? '리뷰를 수정했습니다.' : '리뷰를 등록했습니다.')
    },
    onError: (error) => {
      setErrorMessage(
        error instanceof ReviewApiError
          ? error.message
          : '리뷰를 저장하지 못했습니다. 다시 시도해주세요.',
      )
    },
  })

  const deleteMutation = useMutation({
    mutationFn: ({ reviewId }: { reviewId: number; isMine: boolean }) => (
      deleteStoreReview(storeId, reviewId)
    ),
    onSuccess: (_, variables) => {
      if (variables.isMine) {
        queryClient.setQueryData(['my-store-review', storeId, user?.id], null)
        setRating(5)
        setContent('')
      }
      queryClient.invalidateQueries({ queryKey: ['store-reviews', storeId] })
      setSuccessMessage('리뷰를 삭제했습니다.')
    },
    onError: (error) => {
      setErrorMessage(
        error instanceof ReviewApiError
          ? error.message
          : '리뷰를 삭제하지 못했습니다. 다시 시도해주세요.',
      )
    },
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')
    setSuccessMessage('')

    const normalizedContent = content.trim()
    if (!normalizedContent) {
      setErrorMessage('리뷰 내용을 입력해주세요.')
      return
    }

    saveMutation.mutate({ rating, content: normalizedContent })
  }

  const handleDelete = (review: StoreReview) => {
    if (!window.confirm('이 리뷰를 삭제할까요? 삭제 후에는 복구할 수 없습니다.')) return

    setErrorMessage('')
    setSuccessMessage('')
    deleteMutation.mutate({ reviewId: review.id, isMine: review.mine })
  }

  const reviewPage = reviewsQuery.data

  return (
    <section className="store-review-section" aria-labelledby="store-review-title">
      <div className="review-section-heading">
        <div>
          <span className="section-kicker">Store reviews</span>
          <h2 id="store-review-title">방문자 리뷰</h2>
          <p>점포를 이용한 경험을 별점과 함께 남겨주세요.</p>
        </div>
        <div className="review-score" aria-label={`평균 별점 ${reviewPage?.averageRating.toFixed(1) ?? '0.0'}점`}>
          <Star size={24} fill="currentColor" aria-hidden="true" />
          <strong>{reviewPage?.averageRating.toFixed(1) ?? '0.0'}</strong>
          <span>{reviewPage?.totalElements ?? 0}개 리뷰</span>
        </div>
      </div>

      <div className="review-layout">
        <div className="review-editor-panel">
          {!user ? (
            <div className="review-login-prompt">
              <MessageSquareText size={27} aria-hidden="true" />
              <strong>리뷰를 작성해보세요</strong>
              <p>로그인하면 이 점포에 별점과 이용 후기를 남길 수 있습니다.</p>
              <Link
                to="/login"
                state={{
                  from: location.pathname,
                  message: '점포 리뷰를 작성하려면 먼저 로그인해주세요.',
                }}
              >
                로그인하고 리뷰 작성
              </Link>
            </div>
          ) : myReviewQuery.isLoading ? (
            <div className="review-form-loading" aria-label="작성한 리뷰를 확인하는 중">
              <LoaderCircle className="spin" size={25} aria-hidden="true" />
            </div>
          ) : myReviewQuery.isError ? (
            <div className="review-form-error" role="alert">
              <span>작성한 리뷰를 확인하지 못했습니다.</span>
              <button type="button" onClick={() => myReviewQuery.refetch()}>다시 확인</button>
            </div>
          ) : (
            <form className="review-form" onSubmit={handleSubmit}>
              <div className="review-form-title">
                <div>
                  <strong>{myReviewQuery.data ? '내 리뷰 수정' : '리뷰 작성'}</strong>
                  <span>점포당 한 개의 리뷰를 작성할 수 있어요.</span>
                </div>
                {myReviewQuery.data && <span className="my-review-badge">작성 완료</span>}
              </div>

              <fieldset className="review-rating-field" disabled={saveMutation.isPending}>
                <legend>별점 선택</legend>
                <div className="review-rating-buttons">
                  {Array.from({ length: 5 }, (_, index) => index + 1).map((value) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => setRating(value)}
                      aria-label={`${value}점`}
                      aria-pressed={rating === value}
                    >
                      <Star
                        size={27}
                        fill={value <= rating ? 'currentColor' : 'none'}
                        aria-hidden="true"
                      />
                    </button>
                  ))}
                  <strong>{rating}점</strong>
                </div>
              </fieldset>

              <label className="review-content-field">
                <span>리뷰 내용</span>
                <textarea
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  placeholder="점포의 상품, 서비스, 분위기는 어땠나요?"
                  maxLength={1000}
                  rows={6}
                  disabled={saveMutation.isPending}
                  required
                />
                <small>{content.length} / 1000</small>
              </label>

              {(errorMessage || successMessage) && (
                <p
                  className={`review-form-message ${errorMessage ? 'is-error' : ''}`}
                  role={errorMessage ? 'alert' : 'status'}
                >
                  {errorMessage || successMessage}
                </p>
              )}

              <div className="review-form-actions">
                {myReviewQuery.data && (
                  <button
                    className="review-delete-button"
                    type="button"
                    onClick={() => handleDelete(myReviewQuery.data!)}
                    disabled={deleteMutation.isPending || saveMutation.isPending}
                  >
                    <Trash2 size={16} aria-hidden="true" />
                    리뷰 삭제
                  </button>
                )}
                <button
                  className="review-submit-button"
                  type="submit"
                  disabled={saveMutation.isPending || deleteMutation.isPending}
                >
                  {saveMutation.isPending && (
                    <LoaderCircle className="spin" size={17} aria-hidden="true" />
                  )}
                  {saveMutation.isPending
                    ? '저장 중...'
                    : myReviewQuery.data ? '리뷰 수정' : '리뷰 등록'}
                </button>
              </div>
            </form>
          )}
        </div>

        <div className="review-list-panel">
          {reviewsQuery.isLoading && <ReviewListSkeleton />}

          {reviewsQuery.isError && (
            <div className="review-list-state" role="alert">
              <RefreshCw size={24} aria-hidden="true" />
              <strong>리뷰를 불러오지 못했어요</strong>
              <button type="button" onClick={() => reviewsQuery.refetch()}>다시 불러오기</button>
            </div>
          )}

          {reviewPage?.content.length === 0 && (
            <div className="review-list-state">
              <MessageSquareText size={26} aria-hidden="true" />
              <strong>아직 등록된 리뷰가 없습니다</strong>
              <p>첫 번째 방문 후기를 남겨보세요.</p>
            </div>
          )}

          {reviewPage && reviewPage.content.length > 0 && (
            <>
              <div className="review-list">
                {reviewPage.content.map((review) => (
                  <article className="review-item" key={review.id}>
                    <div className="review-item-heading">
                      <div className="reviewer-profile">
                        <span aria-hidden="true">{review.reviewerName.slice(0, 1)}</span>
                        <div>
                          <strong>{review.reviewerName}</strong>
                          <small>{formatReviewDate(review.updatedAt)}</small>
                        </div>
                      </div>
                      <div className="review-item-tools">
                        {review.mine && <span className="my-review-badge">내 리뷰</span>}
                        {(review.mine || user?.role === 'ADMIN') && (
                          <button
                            type="button"
                            onClick={() => handleDelete(review)}
                            disabled={deleteMutation.isPending
                              && deleteMutation.variables?.reviewId === review.id}
                            aria-label={`${review.reviewerName}님의 리뷰 삭제`}
                          >
                            {deleteMutation.isPending
                              && deleteMutation.variables?.reviewId === review.id
                              ? <LoaderCircle className="spin" size={15} aria-hidden="true" />
                              : <Trash2 size={15} aria-hidden="true" />}
                          </button>
                        )}
                      </div>
                    </div>
                    <ReviewStars rating={review.rating} />
                    <p>{review.content}</p>
                  </article>
                ))}
              </div>

              {reviewPage.totalPages > 1 && (
                <nav className="review-pagination" aria-label="리뷰 목록 페이지">
                  <button
                    type="button"
                    onClick={() => setPage((current) => current - 1)}
                    disabled={reviewPage.first}
                    aria-label="이전 리뷰 페이지"
                  >
                    <ChevronLeft size={17} aria-hidden="true" />
                  </button>
                  <span>{reviewPage.page + 1} / {reviewPage.totalPages}</span>
                  <button
                    type="button"
                    onClick={() => setPage((current) => current + 1)}
                    disabled={reviewPage.last}
                    aria-label="다음 리뷰 페이지"
                  >
                    <ChevronRight size={17} aria-hidden="true" />
                  </button>
                </nav>
              )}
            </>
          )}
        </div>
      </div>
    </section>
  )
}

function ReviewStars({ rating }: { rating: number }) {
  return (
    <div className="review-stars" aria-label={`별점 ${rating}점`}>
      {Array.from({ length: 5 }, (_, index) => (
        <Star
          key={index}
          size={16}
          fill={index < rating ? 'currentColor' : 'none'}
          aria-hidden="true"
        />
      ))}
    </div>
  )
}

function ReviewListSkeleton() {
  return (
    <div className="review-list" aria-label="리뷰를 불러오는 중" aria-busy="true">
      {Array.from({ length: 3 }, (_, index) => (
        <div className="review-item" key={index} aria-hidden="true">
          <div className="skeleton review-skeleton-title" />
          <div className="skeleton review-skeleton-stars" />
          <div className="skeleton review-skeleton-line" />
          <div className="skeleton review-skeleton-line short" />
        </div>
      ))}
    </div>
  )
}

function formatReviewDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '날짜 확인 중'

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date)
}
