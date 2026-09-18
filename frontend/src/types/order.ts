export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELED'

export interface OrderItem {
  orderItemId: number
  productId: number
  productName: string
  productImageUrl: string | null
  unitPrice: number
  quantity: number
  lineAmount: number
}

export interface OrderSummary {
  id: number
  orderNumber: string
  marketId: number
  marketName: string
  storeId: number
  storeName: string
  status: OrderStatus
  totalItemCount: number
  totalAmount: number
  createdAt: string
}

export interface OrderDetail extends OrderSummary {
  items: OrderItem[]
  canceledAt: string | null
  paidAt: string | null
  updatedAt: string
}
