export interface StoreReview {
  id: number
  storeId: number
  reviewerName: string
  rating: number
  content: string
  mine: boolean
  createdAt: string
  updatedAt: string
}

export interface StoreReviewPage {
  content: StoreReview[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  averageRating: number
}

export interface ReviewSaveInput {
  rating: number
  content: string
}
