import type { OrderDetail, OrderSummary } from '../types/order'

export function createOrderDetail(overrides: Partial<OrderDetail> = {}): OrderDetail {
  return {
    id: 50,
    orderNumber: 'ORD-TEST1234',
    marketId: 1,
    marketName: '우리동네 중앙시장',
    storeId: 10,
    storeName: '행복청과',
    status: 'PENDING_PAYMENT',
    items: [
      {
        orderItemId: 500,
        productId: 1,
        productName: '새콤달콤 사과 1kg',
        productImageUrl: null,
        unitPrice: 8500,
        quantity: 2,
        lineAmount: 17000,
      },
    ],
    totalItemCount: 2,
    totalAmount: 17000,
    canceledAt: null,
    createdAt: '2026-09-18T15:30:00',
    updatedAt: '2026-09-18T15:30:00',
    ...overrides,
  }
}

export function createOrderSummary(overrides: Partial<OrderSummary> = {}): OrderSummary {
  const order = createOrderDetail()
  return {
    id: order.id,
    orderNumber: order.orderNumber,
    marketId: order.marketId,
    marketName: order.marketName,
    storeId: order.storeId,
    storeName: order.storeName,
    status: order.status,
    totalItemCount: order.totalItemCount,
    totalAmount: order.totalAmount,
    createdAt: order.createdAt,
    ...overrides,
  }
}
