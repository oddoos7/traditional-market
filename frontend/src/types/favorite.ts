import type { StoreCategory, StoreStatus } from './store'

export interface FavoriteStatus {
  favorite: boolean
}

export interface FavoriteStore {
  favoriteId: number
  favoritedAt: string
  storeId: number
  marketId: number
  marketName: string
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
