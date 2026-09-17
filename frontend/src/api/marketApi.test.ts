import { describe, expect, it, vi } from 'vitest'
import { getAllMarkets, getMarkets } from './marketApi'
import { createMarket } from '../test/marketFixtures'
import type { Market, MarketPage } from '../types/market'

function marketPage(content: Market[], page: number, totalPages: number): MarketPage {
  return {
    content,
    page,
    size: 50,
    totalElements: content.length,
    totalPages,
    first: page === 0,
    last: page === totalPages - 1,
  }
}

function successfulResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

describe('marketApi', () => {
  it('지도용 시장 목록을 모든 페이지에서 불러온다', async () => {
    const firstMarket = createMarket({ id: 1 })
    const secondMarket = createMarket({ id: 2, name: '푸른골목시장' })
    const thirdMarket = createMarket({ id: 3, name: '한빛전통시장' })
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(successfulResponse(marketPage([firstMarket, secondMarket], 0, 2)))
      .mockResolvedValueOnce(successfulResponse(marketPage([thirdMarket], 1, 2)))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getAllMarkets()).resolves.toEqual([
      firstMarket,
      secondMarket,
      thirdMarket,
    ])
    expect(fetchMock).toHaveBeenCalledTimes(2)

    const firstRequest = new URL(fetchMock.mock.calls[0][0], 'http://localhost')
    const secondRequest = new URL(fetchMock.mock.calls[1][0], 'http://localhost')
    expect(firstRequest.searchParams.get('page')).toBe('0')
    expect(firstRequest.searchParams.get('size')).toBe('50')
    expect(firstRequest.searchParams.get('sort')).toBe('NAME')
    expect(secondRequest.searchParams.get('page')).toBe('1')
  })

  it('시장 목록 API 오류를 호출자에게 전달한다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
    }))

    await expect(getMarkets({
      keyword: '',
      status: '',
      page: 0,
      size: 6,
      sort: 'LATEST',
    })).rejects.toThrow('시장 목록을 불러오지 못했습니다. (503)')
  })
})
