import type { Market } from '../types/market'

export function createMarket(overrides: Partial<Market> = {}): Market {
  return {
    id: 1,
    name: '우리동네 중앙시장',
    description: '신선한 먹거리를 만날 수 있는 전통시장입니다.',
    address: '서울특별시 행복구 중앙시장길 12',
    detailAddress: null,
    phone: '02-0000-1001',
    openingHours: '09:00 ~ 20:00',
    closedDays: '매월 둘째 주 일요일',
    latitude: 37.5665,
    longitude: 126.978,
    imageUrl: null,
    status: 'OPEN',
    createdAt: '2026-01-01T09:00:00',
    ...overrides,
  }
}
