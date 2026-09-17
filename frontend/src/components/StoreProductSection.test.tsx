import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getProducts } from '../api/productApi'
import { useAuth } from '../auth/AuthContext'
import { createProductPage, createProductSummary } from '../test/productFixtures'
import { renderWithProviders, screen, userEvent, within } from '../test/render'
import { StoreProductSection } from './StoreProductSection'

vi.mock('../api/productApi', () => ({
  getProducts: vi.fn(),
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

const mockedGetProducts = vi.mocked(getProducts)
const mockedUseAuth = vi.mocked(useAuth)

describe('StoreProductSection', () => {
  beforeEach(() => {
    mockedGetProducts.mockReset()
    mockedUseAuth.mockReturnValue({ user: null } as ReturnType<typeof useAuth>)
  })

  it('상품 목록에 가격, 재고와 판매 상태를 표시한다', async () => {
    mockedGetProducts.mockResolvedValue(createProductPage([
      createProductSummary(),
      createProductSummary({
        id: 2,
        name: '제철 딸기 500g',
        price: 12000,
        stockQuantity: 0,
        status: 'SOLD_OUT',
      }),
    ]))

    renderWithProviders(
      <StoreProductSection marketId={1} storeId={10} storeName="행복청과" />,
    )

    expect(await screen.findByRole('heading', { name: '새콤달콤 사과 1kg' })).toBeInTheDocument()
    const soldOutProduct = screen.getByRole('heading', { name: '제철 딸기 500g' })
      .closest('article')
    expect(soldOutProduct).not.toBeNull()
    expect(screen.getByText('₩8,500')).toBeInTheDocument()
    expect(screen.getByText('재고 30개')).toBeInTheDocument()
    expect(within(soldOutProduct as HTMLElement).getByText('품절')).toBeInTheDocument()
    expect(within(soldOutProduct as HTMLElement).getByText('재고 없음')).toBeInTheDocument()
  })

  it('검색어와 정렬 조건을 변경하면 첫 페이지부터 다시 조회한다', async () => {
    const user = userEvent.setup()
    mockedGetProducts.mockResolvedValue(createProductPage([]))

    renderWithProviders(
      <StoreProductSection marketId={1} storeId={10} storeName="행복청과" />,
    )
    await screen.findByText('아직 등록된 상품이 없어요')

    await user.type(screen.getByRole('searchbox', { name: '상품 검색' }), '사과')
    await user.click(screen.getByRole('button', { name: '검색' }))

    expect(mockedGetProducts).toHaveBeenLastCalledWith(10, expect.objectContaining({
      keyword: '사과',
      page: 0,
    }))

    await user.selectOptions(screen.getByRole('combobox', { name: '상품 정렬' }), 'PRICE_HIGH')
    expect(mockedGetProducts).toHaveBeenLastCalledWith(10, expect.objectContaining({
      keyword: '사과',
      sort: 'PRICE_HIGH',
      page: 0,
    }))
  })

  it('관리자에게만 상품 등록 링크를 표시한다', async () => {
    mockedUseAuth.mockReturnValue({
      user: {
        id: 1,
        email: 'admin@example.com',
        name: '관리자',
        role: 'ADMIN',
        createdAt: '2026-09-18T10:00:00',
      },
    } as ReturnType<typeof useAuth>)
    mockedGetProducts.mockResolvedValue(createProductPage([]))

    renderWithProviders(
      <StoreProductSection marketId={1} storeId={10} storeName="행복청과" />,
    )

    expect(await screen.findByRole('link', { name: '상품 등록' })).toHaveAttribute(
      'href',
      '/admin/markets/1/stores/10/products/new',
    )
  })
})
