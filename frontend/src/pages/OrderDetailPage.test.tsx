import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { cancelOrder, getOrder } from '../api/orderApi'
import { useAuth } from '../auth/AuthContext'
import { createOrderDetail } from '../test/orderFixtures'
import { renderWithProviders, screen, userEvent, waitFor } from '../test/render'
import { OrderDetailPage } from './OrderDetailPage'

vi.mock('../api/orderApi', () => ({
  getOrder: vi.fn(),
  cancelOrder: vi.fn(),
  OrderApiError: class OrderApiError extends Error {
    constructor(message: string, public readonly status: number) { super(message) }
  },
}))
vi.mock('../auth/AuthContext', () => ({ useAuth: vi.fn() }))
vi.mock('../components/TossPaymentWidget', () => ({
  TossPaymentWidget: () => <button type="button">17,000원 결제하기</button>,
}))

const mockedGetOrder = vi.mocked(getOrder)
const mockedCancelOrder = vi.mocked(cancelOrder)
const mockedUseAuth = vi.mocked(useAuth)

function renderDetail() {
  return renderWithProviders(
    <Routes>
      <Route path="/orders/:orderId" element={<OrderDetailPage />} />
    </Routes>,
    { initialEntries: ['/orders/50'] },
  )
}

describe('OrderDetailPage', () => {
  beforeEach(() => {
    mockedGetOrder.mockReset()
    mockedCancelOrder.mockReset()
    mockedUseAuth.mockReturnValue({ user: { id: 1 } } as ReturnType<typeof useAuth>)
  })

  it('주문 상품과 확보된 재고 안내를 표시한다', async () => {
    mockedGetOrder.mockResolvedValue(createOrderDetail())
    renderDetail()

    expect(await screen.findByRole('heading', { name: '주문 상세' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '새콤달콤 사과 1kg' })).toBeInTheDocument()
    expect(screen.getByText('현재 상품 재고가 주문 수량만큼 확보되었습니다.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '17,000원 결제하기' })).toBeEnabled()
  })

  it('결제 대기 주문을 취소하고 상태를 갱신한다', async () => {
    const user = userEvent.setup()
    mockedGetOrder.mockResolvedValue(createOrderDetail())
    mockedCancelOrder.mockResolvedValue(createOrderDetail({
      status: 'CANCELED',
      canceledAt: '2026-09-18T16:00:00',
    }))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderDetail()

    await user.click(await screen.findByRole('button', { name: '주문 취소' }))
    await waitFor(() => expect(mockedCancelOrder).toHaveBeenCalledWith(50))
    expect(await screen.findByText('취소된 주문입니다')).toBeInTheDocument()
  })
})
