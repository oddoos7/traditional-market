export type StoreStatus = 'OPEN' | 'TEMPORARILY_CLOSED' | 'CLOSED'

export type StoreCategory =
  | 'AGRICULTURAL'
  | 'FOOD'
  | 'SEAFOOD'
  | 'LIVESTOCK'
  | 'RESTAURANT'
  | 'CLOTHING'
  | 'HOUSEHOLD'
  | 'ETC'

export interface StoreSummary {
  id: number
  marketId: number
  name: string
  description: string | null
  category: StoreCategory
  location: string | null
  phone: string | null
  openingHours: string | null
  closedDays: string | null
  imageUrl: string | null
  status: StoreStatus
  createdAt: string
}

export interface StoreDetail extends StoreSummary {
  marketName: string
  updatedAt: string
}

export interface StorePage {
  content: StoreSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface StoreQueryParams {
  keyword?: string
  category?: StoreCategory | ''
  status?: StoreStatus | ''
  page?: number
  size?: number
  sort?: 'LATEST' | 'NAME'
}

export interface StoreSaveInput {
  name: string
  description: string | null
  category: StoreCategory
  location: string | null
  phone: string | null
  openingHours: string | null
  closedDays: string | null
  imageUrl: string | null
  status: StoreStatus
}
