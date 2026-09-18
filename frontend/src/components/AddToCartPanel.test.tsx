import { beforeEach, describe, expect, it, vi } from 'vitest'
import { addCartItem } from '../api/cartApi'
import { useAuth } from '../auth/AuthContext'
import { createCart } from '../test/cartFixtures'
import { createProductDetail } from '../test/productFixtures'
import { renderWithProviders, screen, userEvent, waitFor } from '../test/render'
import { AddToCartPanel } from './AddToCartPanel'

vi.mock('../api/cartApi', () => ({
  addCartItem: vi.fn(),
  CartApiError: class CartApiError extends Error {},
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

const mockedAddCartItem = vi.mocked(addCartItem)
const mockedUseAuth = vi.mocked(useAuth)

describe('AddToCartPanel', () => {
  beforeEach(() => {
    mockedAddCartItem.mockReset()
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

  it('선택한 수량으로 상품을 장바구니에 담는다', async () => {
    const user = userEvent.setup()
    mockedAddCartItem.mockResolvedValue(createCart())

    renderWithProviders(<AddToCartPanel product={createProductDetail()} />)

    await user.click(screen.getByRole('button', { name: '수량 늘리기' }))
    await user.click(screen.getByRole('button', { name: '장바구니 담기' }))

    await waitFor(() => expect(mockedAddCartItem).toHaveBeenCalledWith({
      productId: 1,
      quantity: 2,
    }))
    expect(await screen.findByText(/장바구니에 담았습니다/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '장바구니 보기' })).toHaveAttribute('href', '/cart')
  })

  it('비로그인 사용자에게 로그인 이동 링크를 표시한다', () => {
    mockedUseAuth.mockReturnValue({ user: null } as ReturnType<typeof useAuth>)

    renderWithProviders(<AddToCartPanel product={createProductDetail()} />, {
      initialEntries: ['/markets/1/stores/10/products/1'],
    })

    expect(screen.getByText('로그인하면 상품을 장바구니에 담을 수 있습니다.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인하기' })).toHaveAttribute('href', '/login')
  })

  it('품절 상품은 장바구니 버튼을 비활성화한다', () => {
    renderWithProviders(<AddToCartPanel product={createProductDetail({
      status: 'SOLD_OUT',
      stockQuantity: 0,
    })} />)

    expect(screen.getByRole('button', { name: '현재 구매 불가' })).toBeDisabled()
  })
})
