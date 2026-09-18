import { fetchWithCsrf } from './authApi'
import type { Cart, CartItemAddInput } from '../types/cart'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class CartApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'CartApiError'
  }
}

async function toCartApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // JSON 본문이 없는 오류는 화면용 기본 메시지로 처리합니다.
  }

  return new CartApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

export async function getCart(): Promise<Cart> {
  const response = await fetch(`${API_BASE_URL}/cart`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw await toCartApiError(response, '장바구니를 불러오지 못했습니다.')
  }

  return response.json() as Promise<Cart>
}

export async function addCartItem(input: CartItemAddInput): Promise<Cart> {
  const response = await fetchWithCsrf('/cart/items', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toCartApiError(response, '상품을 장바구니에 담지 못했습니다.')
  }

  return response.json() as Promise<Cart>
}

export async function updateCartItemQuantity(
  cartItemId: number,
  quantity: number,
): Promise<Cart> {
  const response = await fetchWithCsrf(`/cart/items/${cartItemId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity }),
  })

  if (!response.ok) {
    throw await toCartApiError(response, '장바구니 수량을 변경하지 못했습니다.')
  }

  return response.json() as Promise<Cart>
}

export async function removeCartItem(cartItemId: number): Promise<Cart> {
  const response = await fetchWithCsrf(`/cart/items/${cartItemId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw await toCartApiError(response, '장바구니 상품을 삭제하지 못했습니다.')
  }

  return response.json() as Promise<Cart>
}

export async function clearCart(): Promise<Cart> {
  const response = await fetchWithCsrf('/cart', { method: 'DELETE' })

  if (!response.ok) {
    throw await toCartApiError(response, '장바구니를 비우지 못했습니다.')
  }

  return response.json() as Promise<Cart>
}
