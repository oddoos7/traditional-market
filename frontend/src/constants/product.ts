import type { ProductSort, ProductStatus } from '../types/product'

export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = {
  ON_SALE: '판매 중',
  SOLD_OUT: '품절',
  HIDDEN: '숨김',
}

export const PRODUCT_SORT_LABELS: Record<ProductSort, string> = {
  LATEST: '최근 등록순',
  NAME: '상품명순',
  PRICE_LOW: '낮은 가격순',
  PRICE_HIGH: '높은 가격순',
}

export function formatProductPrice(price: number) {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(price)
}
