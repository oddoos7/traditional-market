export type ProductStatus = 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN'

export type ProductSort = 'LATEST' | 'NAME' | 'PRICE_LOW' | 'PRICE_HIGH'

export interface ProductSummary {
  id: number
  storeId: number
  name: string
  description: string | null
  price: number
  stockQuantity: number
  imageUrl: string | null
  status: ProductStatus
  createdAt: string
}

export interface ProductDetail extends ProductSummary {
  storeName: string
  marketId: number
  marketName: string
  updatedAt: string
}

export interface ProductPage {
  content: ProductSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface ProductQueryParams {
  keyword?: string
  status?: Exclude<ProductStatus, 'HIDDEN'> | ''
  page?: number
  size?: number
  sort?: ProductSort
}

export interface ProductSaveInput {
  name: string
  description: string | null
  price: number
  stockQuantity: number
  imageUrl: string | null
  status: ProductStatus
}
