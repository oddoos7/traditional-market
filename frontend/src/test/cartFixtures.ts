import type { Cart } from '../types/cart'

export function createCart(overrides: Partial<Cart> = {}): Cart {
  return {
    cartId: 1,
    marketId: 1,
    marketName: '우리동네 중앙시장',
    storeId: 10,
    storeName: '행복청과',
    items: [
      {
        cartItemId: 100,
        productId: 1,
        name: '새콤달콤 사과 1kg',
        unitPrice: 8500,
        quantity: 2,
        lineAmount: 17000,
        stockQuantity: 30,
        imageUrl: null,
        productStatus: 'ON_SALE',
        available: true,
      },
    ],
    totalItemCount: 2,
    totalAmount: 17000,
    orderable: true,
    ...overrides,
  }
}

export function createEmptyCart(): Cart {
  return {
    cartId: null,
    marketId: null,
    marketName: null,
    storeId: null,
    storeName: null,
    items: [],
    totalItemCount: 0,
    totalAmount: 0,
    orderable: false,
  }
}
