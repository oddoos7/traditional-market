import type { ProductDetail, ProductPage, ProductSummary } from '../types/product'

export function createProductSummary(
  overrides: Partial<ProductSummary> = {},
): ProductSummary {
  return {
    id: 1,
    storeId: 10,
    name: '새콤달콤 사과 1kg',
    description: '아삭하고 달콤한 제철 사과입니다.',
    price: 8500,
    stockQuantity: 30,
    imageUrl: null,
    status: 'ON_SALE',
    createdAt: '2026-09-17T10:00:00',
    ...overrides,
  }
}

export function createProductDetail(
  overrides: Partial<ProductDetail> = {},
): ProductDetail {
  return {
    ...createProductSummary(),
    storeName: '행복청과',
    marketId: 1,
    marketName: '우리동네 중앙시장',
    updatedAt: '2026-09-18T10:00:00',
    ...overrides,
  }
}

export function createProductPage(
  content: ProductSummary[] = [createProductSummary()],
  overrides: Partial<ProductPage> = {},
): ProductPage {
  return {
    content,
    page: 0,
    size: 6,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true,
    ...overrides,
  }
}
