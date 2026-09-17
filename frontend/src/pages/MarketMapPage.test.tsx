import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { getAllMarkets } from '../api/marketApi'
import { createMarket } from '../test/marketFixtures'
import { renderWithProviders, screen, userEvent } from '../test/render'

vi.mock('../api/marketApi', () => ({
  getAllMarkets: vi.fn(),
}))

const mockedGetAllMarkets = vi.mocked(getAllMarkets)
let MarketMapPage: typeof import('./MarketMapPage').MarketMapPage

beforeAll(async () => {
  vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', '')
  ;({ MarketMapPage } = await import('./MarketMapPage'))
})

beforeEach(() => {
  mockedGetAllMarkets.mockReset()
})

describe('MarketMapPage', () => {
  it('시장 API 목록을 지도 탐색 목록으로 표시한다', async () => {
    mockedGetAllMarkets.mockResolvedValue([
      createMarket(),
      createMarket({
        id: 2,
        name: '푸른골목시장',
        address: '서울특별시 행복구 푸른길 35',
        latitude: 37.57,
        longitude: 126.982,
      }),
    ])

    renderWithProviders(<MarketMapPage />, { initialEntries: ['/map'] })

    expect(screen.getByRole('heading', { name: '지도에서 시장 찾기' })).toBeInTheDocument()
    expect(await screen.findByText('우리동네 중앙시장')).toBeInTheDocument()
    expect(screen.getByText('푸른골목시장')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: '상세보기' })).toHaveLength(2)
  })

  it('시장명으로 검색하면 해당 시장을 선택한다', async () => {
    const user = userEvent.setup()
    mockedGetAllMarkets.mockResolvedValue([
      createMarket(),
      createMarket({
        id: 2,
        name: '푸른골목시장',
        address: '서울특별시 행복구 푸른길 35',
        latitude: 37.57,
        longitude: 126.982,
      }),
    ])

    renderWithProviders(<MarketMapPage />, { initialEntries: ['/map'] })
    await screen.findByText('푸른골목시장')

    await user.type(screen.getByRole('searchbox', { name: '주소 또는 시장명 검색' }), '푸른')
    await user.click(screen.getByRole('button', { name: '검색' }))

    expect(await screen.findByText("등록된 시장 '푸른골목시장' 위치로 이동했습니다.")).toBeInTheDocument()
    expect(screen.getByText('푸른골목시장').closest('article')).toHaveClass('is-selected')
  })
})
