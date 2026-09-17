import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen } from '../test/render'

describe('MarketLocationMap', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_KAKAO_MAP_APP_KEY', '')
  })

  it('카카오 키가 없으면 설정 안내와 외부 지도 링크를 보여준다', async () => {
    const { MarketLocationMap } = await import('./MarketLocationMap')

    renderWithProviders(
      <MarketLocationMap
        marketName="우리동네 중앙시장"
        address="서울특별시 행복구 중앙시장길 12"
        latitude={37.5665}
        longitude={126.978}
      />,
    )

    expect(screen.getByRole('heading', { name: '우리동네 중앙시장' })).toBeInTheDocument()
    expect(screen.getByText('카카오 지도 키 설정이 필요합니다.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '크게 보기' })).toHaveAttribute(
      'href',
      expect.stringContaining('37.5665,126.978'),
    )
    expect(screen.getByRole('link', { name: '길찾기' })).toBeInTheDocument()
  })

  it('시장 좌표가 없으면 좌표 등록 안내를 보여준다', async () => {
    const { MarketLocationMap } = await import('./MarketLocationMap')

    renderWithProviders(
      <MarketLocationMap
        marketName="좌표 없는 시장"
        address="서울특별시"
        latitude={null}
        longitude={null}
      />,
    )

    expect(screen.getByText('지도에 표시할 좌표가 없습니다.')).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: /카카오 지도/ })).not.toBeInTheDocument()
  })
})
