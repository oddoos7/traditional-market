import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { getProduct, ProductApiError } from '../api/productApi'
import { useAuth } from '../auth/AuthContext'
import { createProductDetail } from '../test/productFixtures'
import { renderWithProviders, screen } from '../test/render'
import { ProductDetailPage } from './ProductDetailPage'

vi.mock('../api/productApi', () => ({
  getProduct: vi.fn(),
  ProductApiError: class ProductApiError extends Error {
    constructor(
      message: string,
      public readonly status: number,
      public readonly code?: string,
    ) {
      super(message)
    }
  },
}))

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

const mockedGetProduct = vi.mocked(getProduct)
const mockedUseAuth = vi.mocked(useAuth)

function renderProductDetail(path = '/markets/1/stores/10/products/1') {
  return renderWithProviders(
    <Routes>
      <Route
        path="/markets/:marketId/stores/:storeId/products/:productId"
        element={<ProductDetailPage />}
      />
    </Routes>,
    { initialEntries: [path] },
  )
}

describe('ProductDetailPage', () => {
  beforeEach(() => {
    mockedGetProduct.mockReset()
    mockedUseAuth.mockReturnValue({ user: null } as ReturnType<typeof useAuth>)
  })

  it('상품 상세 정보와 점포 이동 경로를 표시한다', async () => {
    mockedGetProduct.mockResolvedValue(createProductDetail())

    renderProductDetail()

    expect(await screen.findByRole('heading', { name: '새콤달콤 사과 1kg' })).toBeInTheDocument()
    expect(screen.getByText('₩8,500')).toBeInTheDocument()
    expect(screen.getByText('30개')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /우리동네 중앙시장 · 행복청과/ })).toHaveAttribute(
      'href',
      '/markets/1/stores/10',
    )
    expect(screen.queryByRole('link', { name: '상품 정보 수정' })).not.toBeInTheDocument()
  })

  it('존재하지 않는 상품은 안내 화면을 표시한다', async () => {
    mockedGetProduct.mockRejectedValue(
      new ProductApiError('상품을 찾을 수 없습니다.', 404, 'PRODUCT_NOT_FOUND'),
    )

    renderProductDetail('/markets/1/stores/10/products/999')

    expect(await screen.findByRole('heading', { name: '찾을 수 없는 상품이에요' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '점포로 돌아가기' })).toHaveAttribute(
      'href',
      '/markets/1/stores/10',
    )
  })
})
