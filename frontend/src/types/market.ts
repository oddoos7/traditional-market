export type MarketStatus = 'OPEN' | 'TEMPORARILY_CLOSED' | 'CLOSED'
export type MarketSort = 'LATEST' | 'NAME'

export interface Market {
  id: number
  name: string
  description: string | null
  address: string
  detailAddress: string | null
  phone: string | null
  openingHours: string | null
  closedDays: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  status: MarketStatus
  createdAt: string
}

export interface MarketPage {
  content: Market[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface MarketQueryParams {
  keyword: string
  status: MarketStatus | ''
  page: number
  size: number
  sort: MarketSort
}
