import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithCsrf } from './authApi'
import { addCartItem, CartApiError, getCart, updateCartItemQuantity } from './cartApi'
import { createCart } from '../test/cartFixtures'

vi.mock('./authApi', () => ({
  fetchWithCsrf: vi.fn(),
}))

const mockedFetchWithCsrf = vi.mocked(fetchWithCsrf)

function successfulResponse(body: unknown, status = 200): Response {
  return {
    ok: true,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

describe('cartApi', () => {
  beforeEach(() => {
    mockedFetchWithCsrf.mockReset()
  })

  it('로그인 세션을 포함해 장바구니를 조회한다', async () => {
    const cart = createCart()
    const fetchMock = vi.fn().mockResolvedValue(successfulResponse(cart))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getCart()).resolves.toEqual(cart)
    expect(fetchMock).toHaveBeenCalledWith('/api/cart', { credentials: 'include' })
  })

  it('상품 담기와 수량 변경을 CSRF 보호 요청으로 전송한다', async () => {
    const cart = createCart()
    mockedFetchWithCsrf.mockResolvedValue(successfulResponse(cart, 201))

    await addCartItem({ productId: 1, quantity: 2 })
    expect(mockedFetchWithCsrf).toHaveBeenCalledWith('/cart/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productId: 1, quantity: 2 }),
    })

    mockedFetchWithCsrf.mockResolvedValue(successfulResponse(cart))
    await updateCartItemQuantity(100, 3)
    expect(mockedFetchWithCsrf).toHaveBeenLastCalledWith('/cart/items/100', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ quantity: 3 }),
    })
  })

  it('장바구니 도메인 오류 코드와 메시지를 전달한다', async () => {
    mockedFetchWithCsrf.mockResolvedValue({
      ok: false,
      status: 409,
      json: vi.fn().mockResolvedValue({
        code: 'CART_STORE_MISMATCH',
        message: '장바구니에는 한 점포의 상품만 담을 수 있습니다.',
      }),
    } as unknown as Response)

    const error = await addCartItem({ productId: 2, quantity: 1 }).catch((caught) => caught)

    expect(error).toBeInstanceOf(CartApiError)
    expect(error).toMatchObject({
      status: 409,
      code: 'CART_STORE_MISMATCH',
      message: '장바구니에는 한 점포의 상품만 담을 수 있습니다.',
    })
  })
})
