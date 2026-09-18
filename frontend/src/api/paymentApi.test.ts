import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithCsrf } from './authApi'
import { confirmTossPayment, PaymentApiError } from './paymentApi'

vi.mock('./authApi', () => ({ fetchWithCsrf: vi.fn() }))

const mockedFetchWithCsrf = vi.mocked(fetchWithCsrf)
const input = {
  paymentKey: 'test-payment-key',
  orderId: 'ORD-TEST1234',
  amount: 17000,
}

function response(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: vi.fn().mockResolvedValue(body) } as unknown as Response
}

describe('paymentApi', () => {
  beforeEach(() => mockedFetchWithCsrf.mockReset())

  it('토스 결제 승인 값을 CSRF 보호 요청으로 보낸다', async () => {
    const result = { paymentId: 1, status: 'DONE' }
    mockedFetchWithCsrf.mockResolvedValue(response(result))

    await expect(confirmTossPayment(input)).resolves.toEqual(result)
    expect(mockedFetchWithCsrf).toHaveBeenCalledWith('/payments/toss/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  })

  it('백엔드 결제 오류 코드와 메시지를 보존한다', async () => {
    mockedFetchWithCsrf.mockResolvedValue(response({
      code: 'PAYMENT_AMOUNT_MISMATCH',
      message: '주문 금액과 결제 금액이 일치하지 않습니다.',
    }, false, 409))

    const error = await confirmTossPayment(input).catch((caught) => caught)
    expect(error).toBeInstanceOf(PaymentApiError)
    expect(error).toMatchObject({ status: 409, code: 'PAYMENT_AMOUNT_MISMATCH' })
  })
})
