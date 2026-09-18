import type { ProductStatus } from './product'

export interface CartItem {
  cartItemId: number
  productId: number
  name: string
  unitPrice: number
  quantity: number
  lineAmount: number
  stockQuantity: number
  imageUrl: string | null
  productStatus: ProductStatus
  available: boolean
}

export interface Cart {
  cartId: number | null
  marketId: number | null
  marketName: string | null
  storeId: number | null
  storeName: string | null
  items: CartItem[]
  totalItemCount: number
  totalAmount: number
  orderable: boolean
}

export interface CartItemAddInput {
  productId: number
  quantity: number
}
