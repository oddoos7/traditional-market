import { fetchWithCsrf } from './authApi'
import type {
  ProductDetail,
  ProductPage,
  ProductQueryParams,
  ProductSaveInput,
} from '../types/product'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class ProductApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'ProductApiError'
  }
}

async function toProductApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // JSON 오류 본문이 없으면 화면용 기본 메시지를 사용합니다.
  }

  return new ProductApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

export async function getProducts(
  storeId: number,
  params: ProductQueryParams = {},
): Promise<ProductPage> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 12),
    sort: params.sort ?? 'LATEST',
  })

  if (params.keyword) searchParams.set('keyword', params.keyword)
  if (params.status) searchParams.set('status', params.status)

  const response = await fetch(
    `${API_BASE_URL}/stores/${storeId}/products?${searchParams.toString()}`,
  )

  if (!response.ok) {
    throw await toProductApiError(response, '상품 목록을 불러오지 못했습니다.')
  }

  return response.json() as Promise<ProductPage>
}

export async function getProduct(storeId: number, productId: number): Promise<ProductDetail> {
  const response = await fetch(`${API_BASE_URL}/stores/${storeId}/products/${productId}`)

  if (!response.ok) {
    throw await toProductApiError(response, '상품 상세 정보를 불러오지 못했습니다.')
  }

  return response.json() as Promise<ProductDetail>
}

export async function createProduct(
  storeId: number,
  input: ProductSaveInput,
): Promise<ProductDetail> {
  const response = await fetchWithCsrf(`/stores/${storeId}/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toProductApiError(response, '상품을 등록하지 못했습니다.')
  }

  return response.json() as Promise<ProductDetail>
}

export async function updateProduct(
  storeId: number,
  productId: number,
  input: ProductSaveInput,
): Promise<ProductDetail> {
  const response = await fetchWithCsrf(`/stores/${storeId}/products/${productId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toProductApiError(response, '상품 정보를 수정하지 못했습니다.')
  }

  return response.json() as Promise<ProductDetail>
}
