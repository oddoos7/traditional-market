import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { confirmTossPayment } from '../api/paymentApi'
import { renderWithProviders, screen, waitFor } from '../test/render'
import { PaymentSuccessPage } from './PaymentSuccessPage'

vi.mock('../api/paymentApi', () => ({
  confirmTossPayment: vi.fn(),
  PaymentApiError: class PaymentApiError extends Error {
    constructor(message: string, public readonly status: number) { super(message) }
  },
}))

const mockedConfirmTossPayment = vi.mocked(confirmTossPayment)

function renderSuccess(search = '?paymentKey=pay_key&orderId=ORD-TEST1234&amount=17000') {
  return renderWithProviders(
    <Routes>
      <Route path="/payments/toss/success" element={<PaymentSuccessPage />} />
    </Routes>,
    { initialEntries: [`/payments/toss/success${search}`] },
  )
}

describe('PaymentSuccessPage', () => {
  beforeEach(() => mockedConfirmTossPayment.mockReset())

  it('URL 결제 정보를 백엔드에서 승인하고 완료 결과를 표시한다', async () => {
    mockedConfirmTossPayment.mockResolvedValue({
      paymentId: 7,
      orderId: 50,
      orderNumber: 'ORD-TEST1234',
      paymentKey: 'pay_key',
      status: 'DONE',
      method: '카드',
      amount: 17000,
      balanceAmount: 17000,
      approvedAt: '2026-09-18T16:30:00',
      canceledAt: null,
      receiptUrl: 'https://example.com/receipt',
    })
    renderSuccess()

    await waitFor(() => expect(mockedConfirmTossPayment).toHaveBeenCalledTimes(1))
    expect(mockedConfirmTossPayment.mock.calls[0][0]).toEqual({
      paymentKey: 'pay_key',
      orderId: 'ORD-TEST1234',
      amount: 17000,
    })
    expect(await screen.findByRole('heading', { name: '테스트 결제가 완료됐어요' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /주문 상세 보기/ })).toHaveAttribute('href', '/orders/50')
  })

  it('필수 성공 파라미터가 변조되면 승인 API를 호출하지 않는다', () => {
    renderSuccess('?paymentKey=pay_key&orderId=wrong!&amount=-1')

    expect(screen.getByRole('heading', { name: '결제 정보를 확인할 수 없어요' })).toBeInTheDocument()
    expect(mockedConfirmTossPayment).not.toHaveBeenCalled()
  })
})
