import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithCsrf } from './authApi'
import { cancelOrder, createOrder, getOrder, getOrders, OrderApiError } from './orderApi'
import { createOrderDetail, createOrderSummary } from '../test/orderFixtures'

vi.mock('./authApi', () => ({ fetchWithCsrf: vi.fn() }))

const mockedFetchWithCsrf = vi.mocked(fetchWithCsrf)

function response(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: vi.fn().mockResolvedValue(body) } as unknown as Response
}

describe('orderApi', () => {
  beforeEach(() => {
    mockedFetchWithCsrf.mockReset()
    vi.unstubAllGlobals()
  })

  it('주문 목록과 상세를 로그인 세션으로 조회한다', async () => {
    const detail = createOrderDetail()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response([createOrderSummary()]))
      .mockResolvedValueOnce(response(detail))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getOrders()).resolves.toHaveLength(1)
    await expect(getOrder(50)).resolves.toEqual(detail)
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/orders', { credentials: 'include' })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/orders/50', { credentials: 'include' })
  })

  it('주문 생성과 취소를 CSRF 보호 요청으로 전송한다', async () => {
    const detail = createOrderDetail()
    mockedFetchWithCsrf.mockResolvedValue(response(detail, true, 201))

    await createOrder()
    expect(mockedFetchWithCsrf).toHaveBeenCalledWith('/orders', { method: 'POST' })

    mockedFetchWithCsrf.mockResolvedValue(response({ ...detail, status: 'CANCELED' }))
    await cancelOrder(50)
    expect(mockedFetchWithCsrf).toHaveBeenLastCalledWith('/orders/50/cancel', {
      method: 'POST',
    })
  })

  it('재고 부족 오류 코드를 보존한다', async () => {
    mockedFetchWithCsrf.mockResolvedValue(response({
      code: 'ORDER_STOCK_UNAVAILABLE',
      message: '주문 가능한 재고가 부족합니다.',
    }, false, 409))

    const error = await createOrder().catch((caught) => caught)
    expect(error).toBeInstanceOf(OrderApiError)
    expect(error).toMatchObject({ status: 409, code: 'ORDER_STOCK_UNAVAILABLE' })
  })
})
