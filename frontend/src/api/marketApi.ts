import type { MarketDetail, MarketPage, MarketQueryParams } from '../types/market'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export class MarketApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message)
    this.name = 'MarketApiError'
  }
}

export async function getMarkets(params: MarketQueryParams): Promise<MarketPage> {
  const searchParams = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    sort: params.sort,
  })

  if (params.keyword) searchParams.set('keyword', params.keyword)
  if (params.status) searchParams.set('status', params.status)

  const response = await fetch(`${API_BASE_URL}/markets?${searchParams.toString()}`)

  if (!response.ok) {
    throw new Error(`시장 목록을 불러오지 못했습니다. (${response.status})`)
  }

  return response.json() as Promise<MarketPage>
}

export async function getMarket(marketId: number): Promise<MarketDetail> {
  const response = await fetch(`${API_BASE_URL}/markets/${marketId}`)

  if (!response.ok) {
    throw new MarketApiError(
      `시장 상세 정보를 불러오지 못했습니다. (${response.status})`,
      response.status,
    )
  }

  return response.json() as Promise<MarketDetail>
}
