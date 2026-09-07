import type { StoreCategory, StoreStatus } from '../types/store'

export const STORE_CATEGORY_LABELS: Record<StoreCategory, string> = {
  AGRICULTURAL: '농산물',
  FOOD: '식품·반찬',
  SEAFOOD: '수산물',
  LIVESTOCK: '축산물',
  RESTAURANT: '음식점',
  CLOTHING: '의류',
  HOUSEHOLD: '생활용품',
  ETC: '기타',
}

export const STORE_STATUS_LABELS: Record<StoreStatus, string> = {
  OPEN: '영업 중',
  TEMPORARILY_CLOSED: '임시 휴무',
  CLOSED: '영업 종료',
}
