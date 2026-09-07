import { fetchWithCsrf } from './authApi'
import type {
  StoreDetail,
  StorePage,
  StoreQueryParams,
  StoreSaveInput,
} from '../types/store'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class StoreApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'StoreApiError'
  }
}

async function toStoreApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // 응답 본문이 없으면 호출 지점에서 전달한 기본 문구를 사용합니다.
  }

  return new StoreApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

export async function getStores(
  marketId: number,
  params: StoreQueryParams = {},
): Promise<StorePage> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 12),
    sort: params.sort ?? 'LATEST',
  })

  if (params.keyword) searchParams.set('keyword', params.keyword)
  if (params.category) searchParams.set('category', params.category)
  if (params.status) searchParams.set('status', params.status)

  const response = await fetch(
    `${API_BASE_URL}/markets/${marketId}/stores?${searchParams.toString()}`,
  )

  if (!response.ok) {
    throw await toStoreApiError(response, '점포 목록을 불러오지 못했습니다.')
  }

  return response.json() as Promise<StorePage>
}

export async function getStore(marketId: number, storeId: number): Promise<StoreDetail> {
  const response = await fetch(`${API_BASE_URL}/markets/${marketId}/stores/${storeId}`)

  if (!response.ok) {
    throw await toStoreApiError(response, '점포 상세 정보를 불러오지 못했습니다.')
  }

  return response.json() as Promise<StoreDetail>
}

export async function createStore(
  marketId: number,
  input: StoreSaveInput,
): Promise<StoreDetail> {
  const response = await fetchWithCsrf(`/markets/${marketId}/stores`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toStoreApiError(response, '점포를 등록하지 못했습니다.')
  }

  return response.json() as Promise<StoreDetail>
}

export async function updateStore(
  marketId: number,
  storeId: number,
  input: StoreSaveInput,
): Promise<StoreDetail> {
  const response = await fetchWithCsrf(`/markets/${marketId}/stores/${storeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toStoreApiError(response, '점포 정보를 수정하지 못했습니다.')
  }

  return response.json() as Promise<StoreDetail>
}
