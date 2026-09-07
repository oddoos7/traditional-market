import { fetchWithCsrf } from './authApi'
import type { FavoriteStatus, FavoriteStore } from '../types/favorite'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class FavoriteApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'FavoriteApiError'
  }
}

async function toFavoriteApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // 본문이 없는 오류 응답은 사용자가 이해하기 쉬운 기본 문구로 처리합니다.
  }

  return new FavoriteApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

export async function getFavoriteStores(): Promise<FavoriteStore[]> {
  const response = await fetch(`${API_BASE_URL}/favorites/stores`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw await toFavoriteApiError(response, '찜한 점포를 불러오지 못했습니다.')
  }

  return response.json() as Promise<FavoriteStore[]>
}

export async function getFavoriteStatus(storeId: number): Promise<FavoriteStatus> {
  const response = await fetch(`${API_BASE_URL}/favorites/stores/${storeId}/status`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw await toFavoriteApiError(response, '찜 상태를 확인하지 못했습니다.')
  }

  return response.json() as Promise<FavoriteStatus>
}

export async function addFavorite(storeId: number): Promise<FavoriteStatus> {
  const response = await fetchWithCsrf(`/favorites/stores/${storeId}`, {
    method: 'POST',
  })

  if (!response.ok) {
    throw await toFavoriteApiError(response, '찜을 추가하지 못했습니다.')
  }

  return response.json() as Promise<FavoriteStatus>
}

export async function removeFavorite(storeId: number): Promise<void> {
  const response = await fetchWithCsrf(`/favorites/stores/${storeId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw await toFavoriteApiError(response, '찜을 해제하지 못했습니다.')
  }
}
