import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithCsrf } from './authApi'
import { createProduct, getProducts, ProductApiError } from './productApi'
import { createProductDetail, createProductPage } from '../test/productFixtures'
import type { ProductSaveInput } from '../types/product'

vi.mock('./authApi', () => ({
  fetchWithCsrf: vi.fn(),
}))

const mockedFetchWithCsrf = vi.mocked(fetchWithCsrf)

function successfulResponse(body: unknown, status = 200): Response {
  return {
    ok: true,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

describe('productApi', () => {
  beforeEach(() => {
    mockedFetchWithCsrf.mockReset()
  })

  it('상품 목록 조회 조건을 API 쿼리 파라미터로 전달한다', async () => {
    const page = createProductPage()
    const fetchMock = vi.fn().mockResolvedValue(successfulResponse(page))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getProducts(10, {
      keyword: '사과',
      status: 'ON_SALE',
      page: 2,
      size: 6,
      sort: 'PRICE_LOW',
    })).resolves.toEqual(page)

    const requestUrl = new URL(fetchMock.mock.calls[0][0], 'http://localhost')
    expect(requestUrl.pathname).toBe('/api/stores/10/products')
    expect(requestUrl.searchParams.get('keyword')).toBe('사과')
    expect(requestUrl.searchParams.get('status')).toBe('ON_SALE')
    expect(requestUrl.searchParams.get('page')).toBe('2')
    expect(requestUrl.searchParams.get('size')).toBe('6')
    expect(requestUrl.searchParams.get('sort')).toBe('PRICE_LOW')
  })

  it('서버 오류 코드와 메시지를 ProductApiError로 전달한다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: vi.fn().mockResolvedValue({
        code: 'STORE_NOT_FOUND',
        message: '점포를 찾을 수 없습니다.',
      }),
    }))

    const error = await getProducts(999).catch((caught) => caught)

    expect(error).toBeInstanceOf(ProductApiError)
    expect(error).toMatchObject({
      status: 404,
      code: 'STORE_NOT_FOUND',
      message: '점포를 찾을 수 없습니다.',
    })
  })

  it('상품 등록 요청을 CSRF 보호 요청으로 전송한다', async () => {
    const input: ProductSaveInput = {
      name: '새콤달콤 사과 1kg',
      description: '제철 사과',
      price: 8500,
      stockQuantity: 30,
      imageUrl: null,
      status: 'ON_SALE',
    }
    const product = createProductDetail()
    mockedFetchWithCsrf.mockResolvedValue(successfulResponse(product, 201))

    await expect(createProduct(10, input)).resolves.toEqual(product)
    expect(mockedFetchWithCsrf).toHaveBeenCalledWith('/stores/10/products', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  })
})
