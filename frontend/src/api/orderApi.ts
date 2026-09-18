import { fetchWithCsrf } from './authApi'
import type { OrderDetail, OrderSummary } from '../types/order'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class OrderApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'OrderApiError'
  }
}

async function toOrderApiError(response: Response, fallbackMessage: string) {
  let body: ApiErrorBody = {}
  try {
    body = await response.json() as ApiErrorBody
  } catch {
    // 오류 본문이 JSON이 아니면 화면용 기본 메시지를 사용합니다.
  }
  return new OrderApiError(body.message || fallbackMessage, response.status, body.code)
}

export async function createOrder(): Promise<OrderDetail> {
  const response = await fetchWithCsrf('/orders', { method: 'POST' })
  if (!response.ok) {
    throw await toOrderApiError(response, '주문을 생성하지 못했습니다.')
  }
  return response.json() as Promise<OrderDetail>
}

export async function getOrders(): Promise<OrderSummary[]> {
  const response = await fetch(`${API_BASE_URL}/orders`, { credentials: 'include' })
  if (!response.ok) {
    throw await toOrderApiError(response, '주문 내역을 불러오지 못했습니다.')
  }
  return response.json() as Promise<OrderSummary[]>
}

export async function getOrder(orderId: number): Promise<OrderDetail> {
  const response = await fetch(`${API_BASE_URL}/orders/${orderId}`, {
    credentials: 'include',
  })
  if (!response.ok) {
    throw await toOrderApiError(response, '주문 상세를 불러오지 못했습니다.')
  }
  return response.json() as Promise<OrderDetail>
}

export async function cancelOrder(orderId: number): Promise<OrderDetail> {
  const response = await fetchWithCsrf(`/orders/${orderId}/cancel`, { method: 'POST' })
  if (!response.ok) {
    throw await toOrderApiError(response, '주문을 취소하지 못했습니다.')
  }
  return response.json() as Promise<OrderDetail>
}
