import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getOrders } from '../api/orderApi'
import { useAuth } from '../auth/AuthContext'
import { createOrderSummary } from '../test/orderFixtures'
import { renderWithProviders, screen } from '../test/render'
import { OrdersPage } from './OrdersPage'

vi.mock('../api/orderApi', () => ({ getOrders: vi.fn() }))
vi.mock('../auth/AuthContext', () => ({ useAuth: vi.fn() }))

const mockedGetOrders = vi.mocked(getOrders)
const mockedUseAuth = vi.mocked(useAuth)

describe('OrdersPage', () => {
  beforeEach(() => {
    mockedGetOrders.mockReset()
    mockedUseAuth.mockReturnValue({ user: { id: 1 } } as ReturnType<typeof useAuth>)
  })

  it('주문 상태와 금액, 상세 경로를 표시한다', async () => {
    mockedGetOrders.mockResolvedValue([createOrderSummary()])
    renderWithProviders(<OrdersPage />)

    expect(await screen.findByText('행복청과')).toBeInTheDocument()
    expect(screen.getByText('결제 대기')).toBeInTheDocument()
    expect(screen.getByText('₩17,000')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /주문 상세 보기/ })).toHaveAttribute(
      'href',
      '/orders/50',
    )
  })

  it('주문이 없으면 탐색 안내를 표시한다', async () => {
    mockedGetOrders.mockResolvedValue([])
    renderWithProviders(<OrdersPage />)
    expect(await screen.findByRole('heading', { name: '아직 주문 내역이 없어요' })).toBeInTheDocument()
  })
})
