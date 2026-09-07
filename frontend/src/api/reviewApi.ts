import { fetchWithCsrf } from './authApi'
import type {
  ReviewSaveInput,
  StoreReview,
  StoreReviewPage,
} from '../types/review'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class ReviewApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'ReviewApiError'
  }
}

async function toReviewApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // JSON 오류 본문이 없으면 호출 지점의 기본 문구를 사용합니다.
  }

  return new ReviewApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

export async function getStoreReviews(
  storeId: number,
  page = 0,
  size = 5,
): Promise<StoreReviewPage> {
  const response = await fetch(
    `${API_BASE_URL}/stores/${storeId}/reviews?page=${page}&size=${size}`,
    { credentials: 'include' },
  )

  if (!response.ok) {
    throw await toReviewApiError(response, '리뷰를 불러오지 못했습니다.')
  }

  return response.json() as Promise<StoreReviewPage>
}

export async function getMyStoreReview(storeId: number): Promise<StoreReview | null> {
  const response = await fetch(`${API_BASE_URL}/stores/${storeId}/reviews/me`, {
    credentials: 'include',
  })

  if (response.status === 204) return null

  if (!response.ok) {
    throw await toReviewApiError(response, '작성한 리뷰를 확인하지 못했습니다.')
  }

  return response.json() as Promise<StoreReview>
}

export async function createStoreReview(
  storeId: number,
  input: ReviewSaveInput,
): Promise<StoreReview> {
  const response = await fetchWithCsrf(`/stores/${storeId}/reviews`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toReviewApiError(response, '리뷰를 등록하지 못했습니다.')
  }

  return response.json() as Promise<StoreReview>
}

export async function updateStoreReview(
  storeId: number,
  reviewId: number,
  input: ReviewSaveInput,
): Promise<StoreReview> {
  const response = await fetchWithCsrf(`/stores/${storeId}/reviews/${reviewId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toReviewApiError(response, '리뷰를 수정하지 못했습니다.')
  }

  return response.json() as Promise<StoreReview>
}

export async function deleteStoreReview(storeId: number, reviewId: number): Promise<void> {
  const response = await fetchWithCsrf(`/stores/${storeId}/reviews/${reviewId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw await toReviewApiError(response, '리뷰를 삭제하지 못했습니다.')
  }
}
