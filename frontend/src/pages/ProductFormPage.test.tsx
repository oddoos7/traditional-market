import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { createProduct, getProduct, updateProduct } from '../api/productApi'
import { getStore } from '../api/storeApi'
import { createProductDetail } from '../test/productFixtures'
import { renderWithProviders, screen, userEvent, waitFor } from '../test/render'
import { ProductFormPage } from './ProductFormPage'

vi.mock('../api/productApi', () => ({
  createProduct: vi.fn(),
  getProduct: vi.fn(),
  updateProduct: vi.fn(),
  ProductApiError: class ProductApiError extends Error {},
}))

vi.mock('../api/storeApi', () => ({
  getStore: vi.fn(),
}))

const mockedCreateProduct = vi.mocked(createProduct)
const mockedGetProduct = vi.mocked(getProduct)
const mockedUpdateProduct = vi.mocked(updateProduct)
const mockedGetStore = vi.mocked(getStore)

describe('ProductFormPage', () => {
  beforeEach(() => {
    mockedCreateProduct.mockReset()
    mockedGetProduct.mockReset()
    mockedUpdateProduct.mockReset()
    mockedGetStore.mockReset()
    mockedGetStore.mockResolvedValue({
      id: 10,
      marketId: 1,
      marketName: '우리동네 중앙시장',
      name: '행복청과',
      description: null,
      category: 'AGRICULTURAL',
      location: '가동 12호',
      phone: null,
      openingHours: null,
      closedDays: null,
      imageUrl: null,
      status: 'OPEN',
      createdAt: '2026-09-17T10:00:00',
      updatedAt: '2026-09-18T10:00:00',
    })
  })

  it('입력한 상품 정보를 등록하고 상세 화면으로 이동한다', async () => {
    const user = userEvent.setup()
    mockedCreateProduct.mockResolvedValue(createProductDetail())

    renderWithProviders(
      <Routes>
        <Route
          path="/admin/markets/:marketId/stores/:storeId/products/new"
          element={<ProductFormPage />}
        />
        <Route
          path="/markets/:marketId/stores/:storeId/products/:productId"
          element={<p>상품 등록 완료</p>}
        />
      </Routes>,
      { initialEntries: ['/admin/markets/1/stores/10/products/new'] },
    )

    expect(await screen.findByText(/행복청과/)).toBeInTheDocument()
    await user.type(screen.getByLabelText(/상품명/), '새콤달콤 사과 1kg')
    await user.type(screen.getByLabelText(/상품 설명/), '아삭한 제철 사과')
    await user.type(screen.getByLabelText(/판매 가격/), '8500')
    await user.clear(screen.getByLabelText(/재고 수량/))
    await user.type(screen.getByLabelText(/재고 수량/), '30')
    await user.click(screen.getByRole('button', { name: '상품 등록' }))

    await waitFor(() => expect(mockedCreateProduct).toHaveBeenCalledWith(10, {
      name: '새콤달콤 사과 1kg',
      description: '아삭한 제철 사과',
      price: 8500,
      stockQuantity: 30,
      imageUrl: null,
      status: 'ON_SALE',
    }))
    expect(await screen.findByText('상품 등록 완료')).toBeInTheDocument()
  })
})
