import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearCart,
  getCart,
  removeCartItem,
  updateCartItemQuantity,
} from '../api/cartApi'
import { useAuth } from '../auth/AuthContext'
import { createCart, createEmptyCart } from '../test/cartFixtures'
import { renderWithProviders, screen, userEvent, waitFor } from '../test/render'
import { CartPage } from './CartPage'

vi.mock('../api/cartApi', () => ({
  clearCart: vi.fn(),
  getCart: vi.fn(),
  removeCartItem: vi.fn(),
  updateCartItemQuantity: vi.fn(),
  CartApiError: class CartApiError extends Error {},
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

const mockedClearCart = vi.mocked(clearCart)
const mockedGetCart = vi.mocked(getCart)
const mockedRemoveCartItem = vi.mocked(removeCartItem)
const mockedUpdateQuantity = vi.mocked(updateCartItemQuantity)
const mockedUseAuth = vi.mocked(useAuth)

describe('CartPage', () => {
  beforeEach(() => {
    mockedClearCart.mockReset()
    mockedGetCart.mockReset()
    mockedRemoveCartItem.mockReset()
    mockedUpdateQuantity.mockReset()
    mockedUseAuth.mockReturnValue({
      user: {
        id: 1,
        email: 'member@example.com',
        name: '시장사랑',
        role: 'USER',
        createdAt: '2026-09-18T10:00:00',
      },
    } as ReturnType<typeof useAuth>)
  })

  it('장바구니 상품과 합계 금액을 표시하고 수량을 변경한다', async () => {
    const user = userEvent.setup()
    const cart = createCart()
    mockedGetCart.mockResolvedValue(cart)
    mockedUpdateQuantity.mockResolvedValue(createCart({
      items: [{ ...cart.items[0], quantity: 3, lineAmount: 25500 }],
      totalItemCount: 3,
      totalAmount: 25500,
    }))

    renderWithProviders(<CartPage />, { initialEntries: ['/cart'] })

    expect(await screen.findByRole('link', { name: '새콤달콤 사과 1kg' })).toBeInTheDocument()
    expect(screen.getAllByText('₩17,000')).toHaveLength(3)
    expect(screen.getByText('행복청과')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '새콤달콤 사과 1kg 수량 늘리기' }))
    await waitFor(() => expect(mockedUpdateQuantity).toHaveBeenCalledWith(100, 3))
    expect(await screen.findByText('3개')).toBeInTheDocument()
    expect(screen.getAllByText('₩25,500')).toHaveLength(3)
  })

  it('빈 장바구니는 시장 탐색 안내를 표시한다', async () => {
    mockedGetCart.mockResolvedValue(createEmptyCart())

    renderWithProviders(<CartPage />)

    expect(await screen.findByRole('heading', { name: '장바구니가 비어 있어요' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /시장 둘러보기/ })).toHaveAttribute('href', '/')
  })
})
