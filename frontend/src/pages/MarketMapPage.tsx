import { useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  LoaderCircle,
  LocateFixed,
  MapPinned,
  MapPin,
  RefreshCw,
  Search,
  SearchX,
  Store,
  TriangleAlert,
} from 'lucide-react'
import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllMarkets } from '../api/marketApi'
import {
  loadKakaoMaps,
  type KakaoMapInstance,
  type KakaoMapsSdk,
  type KakaoMarkerInstance,
} from '../lib/kakaoMaps'
import type { Market, MarketStatus } from '../types/market'

const KAKAO_MAP_APP_KEY = import.meta.env.VITE_KAKAO_MAP_APP_KEY?.trim()

const STATUS_LABELS: Record<MarketStatus, string> = {
  OPEN: '운영 중',
  TEMPORARILY_CLOSED: '임시 휴무',
  CLOSED: '운영 종료',
}

type MapLoadStatus = 'loading' | 'ready' | 'missing-key' | 'error'
type SearchStatus = 'idle' | 'searching' | 'success' | 'empty' | 'error'

interface SearchFeedback {
  status: SearchStatus
  message: string
}

export function MarketMapPage() {
  const marketsQuery = useQuery({
    queryKey: ['markets', 'map'],
    queryFn: getAllMarkets,
    staleTime: 60_000,
  })

  useEffect(() => {
    document.title = '시장 지도 | 우리동네 전통시장'
  }, [])

  return (
    <main className="market-map-main">
      <section className="market-map-heading" aria-labelledby="market-map-page-title">
        <div>
          <span className="eyebrow">Explore by map</span>
          <h1 id="market-map-page-title">지도에서 시장 찾기</h1>
          <p>주소를 검색하거나 목록의 시장을 선택해 위치를 확인해보세요.</p>
        </div>
        <div className="market-map-heading-icon" aria-hidden="true">
          <MapPinned size={30} />
        </div>
      </section>

      {marketsQuery.isLoading && (
        <div className="market-map-page-state" role="status">
          <LoaderCircle className="spin" size={30} aria-hidden="true" />
          <strong>지도에 표시할 시장을 불러오고 있어요.</strong>
        </div>
      )}

      {marketsQuery.isError && (
        <div className="market-map-page-state market-map-page-error" role="alert">
          <TriangleAlert size={30} aria-hidden="true" />
          <strong>시장 목록을 불러오지 못했습니다.</strong>
          <button type="button" onClick={() => marketsQuery.refetch()}>
            <RefreshCw size={16} aria-hidden="true" />
            다시 불러오기
          </button>
        </div>
      )}

      {marketsQuery.data && (
        <MarketMapExplorer markets={marketsQuery.data} />
      )}
    </main>
  )
}

function MarketMapExplorer({ markets }: { markets: Market[] }) {
  const mappableMarkets = useMemo(
    () => markets.filter(
      (market) => market.latitude !== null && market.longitude !== null,
    ),
    [markets],
  )
  const mapContainerRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<KakaoMapInstance | null>(null)
  const mapsSdkRef = useRef<KakaoMapsSdk | null>(null)
  const marketMarkersRef = useRef<KakaoMarkerInstance[]>([])
  const searchMarkerRef = useRef<KakaoMarkerInstance | null>(null)
  const searchSequenceRef = useRef(0)
  const [mapStatus, setMapStatus] = useState<MapLoadStatus>(
    KAKAO_MAP_APP_KEY ? 'loading' : 'missing-key',
  )
  const [selectedMarketId, setSelectedMarketId] = useState<number | null>(
    mappableMarkets[0]?.id ?? null,
  )
  const [addressInput, setAddressInput] = useState('')
  const [searchFeedback, setSearchFeedback] = useState<SearchFeedback>({
    status: 'idle',
    message: '',
  })

  useEffect(() => {
    if (selectedMarketId !== null && !mappableMarkets.some((market) => market.id === selectedMarketId)) {
      setSelectedMarketId(mappableMarkets[0]?.id ?? null)
    }
  }, [mappableMarkets, selectedMarketId])

  useEffect(() => {
    const container = mapContainerRef.current
    if (!container || mappableMarkets.length === 0 || !KAKAO_MAP_APP_KEY) return

    let cancelled = false
    setMapStatus('loading')

    loadKakaoMaps(KAKAO_MAP_APP_KEY)
      .then((maps) => {
        if (cancelled) return

        const firstMarket = mappableMarkets[0]
        const firstPosition = new maps.LatLng(firstMarket.latitude!, firstMarket.longitude!)
        const map = new maps.Map(container, {
          center: firstPosition,
          level: 6,
        })
        const bounds = new maps.LatLngBounds()

        mapsSdkRef.current = maps
        mapInstanceRef.current = map
        marketMarkersRef.current = mappableMarkets.map((market) => {
          const position = new maps.LatLng(market.latitude!, market.longitude!)
          const marker = new maps.Marker({ position, title: market.name })

          marker.setMap(map)
          bounds.extend(position)
          maps.event.addListener(marker, 'click', () => {
            setSelectedMarketId(market.id)
            setSearchFeedback({ status: 'idle', message: '' })
            searchMarkerRef.current?.setMap(null)
            map.panTo(position)
            map.setLevel(3)
            requestAnimationFrame(() => {
              document.getElementById(`market-map-item-${market.id}`)?.scrollIntoView({
                block: 'nearest',
                behavior: 'smooth',
              })
            })
          })

          return marker
        })

        map.addControl(new maps.ZoomControl(), maps.ControlPosition.RIGHT)
        if (mappableMarkets.length > 1) {
          map.setBounds(bounds, 64)
        } else {
          map.setLevel(3)
        }
        setMapStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setMapStatus('error')
      })

    return () => {
      cancelled = true
      searchSequenceRef.current += 1
      searchMarkerRef.current?.setMap(null)
      marketMarkersRef.current.forEach((marker) => marker.setMap(null))
      marketMarkersRef.current = []
      searchMarkerRef.current = null
      mapInstanceRef.current = null
      mapsSdkRef.current = null
      container.replaceChildren()
    }
  }, [mappableMarkets])

  const focusMarket = (market: Market) => {
    setSelectedMarketId(market.id)
    searchMarkerRef.current?.setMap(null)
    setSearchFeedback({ status: 'idle', message: '' })

    const maps = mapsSdkRef.current
    const map = mapInstanceRef.current
    if (!maps || !map || market.latitude === null || market.longitude === null) return

    map.panTo(new maps.LatLng(market.latitude, market.longitude))
    map.setLevel(3)
  }

  const handleAddressSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const query = addressInput.trim()

    if (!query) {
      setSearchFeedback({ status: 'empty', message: '검색할 주소나 시장명을 입력해주세요.' })
      return
    }

    const normalizedQuery = query.toLocaleLowerCase('ko-KR')
    const matchedMarket = mappableMarkets.find((market) => (
      market.name.toLocaleLowerCase('ko-KR').includes(normalizedQuery)
      || market.address.toLocaleLowerCase('ko-KR').includes(normalizedQuery)
      || market.detailAddress?.toLocaleLowerCase('ko-KR').includes(normalizedQuery)
    ))

    if (matchedMarket) {
      focusMarket(matchedMarket)
      setSearchFeedback({
        status: 'success',
        message: `등록된 시장 '${matchedMarket.name}' 위치로 이동했습니다.`,
      })
      return
    }

    const maps = mapsSdkRef.current
    const map = mapInstanceRef.current
    if (!maps || !map) {
      setSearchFeedback({
        status: 'error',
        message: '지도가 준비된 후 주소를 검색할 수 있습니다.',
      })
      return
    }

    const searchSequence = searchSequenceRef.current + 1
    searchSequenceRef.current = searchSequence
    setSearchFeedback({ status: 'searching', message: '주소를 검색하고 있어요.' })

    const geocoder = new maps.services.Geocoder()
    geocoder.addressSearch(query, (results, status) => {
      if (searchSequence !== searchSequenceRef.current) return

      if (status === maps.services.Status.OK && results[0]) {
        const latitude = Number(results[0].y)
        const longitude = Number(results[0].x)
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
          setSearchFeedback({ status: 'error', message: '검색 좌표를 확인하지 못했습니다.' })
          return
        }

        const position = new maps.LatLng(latitude, longitude)
        searchMarkerRef.current?.setMap(null)
        const marker = new maps.Marker({ position, title: '검색한 주소' })
        marker.setMap(map)
        searchMarkerRef.current = marker
        setSelectedMarketId(null)
        map.panTo(position)
        map.setLevel(3)
        setSearchFeedback({
          status: 'success',
          message: results[0].road_address?.address_name || results[0].address_name,
        })
        return
      }

      setSearchFeedback({
        status: status === maps.services.Status.ZERO_RESULT ? 'empty' : 'error',
        message: status === maps.services.Status.ZERO_RESULT
          ? '일치하는 주소를 찾지 못했습니다.'
          : '주소 검색 중 문제가 발생했습니다.',
      })
    })
  }

  if (mappableMarkets.length === 0) {
    return (
      <div className="market-map-page-state">
        <MapPin size={30} aria-hidden="true" />
        <strong>지도에 표시할 시장 좌표가 없습니다.</strong>
        <span>시장 정보에 위도와 경도를 등록해주세요.</span>
      </div>
    )
  }

  return (
    <section className="market-map-explorer" aria-label="시장 지도와 목록">
      <aside className="map-market-list-panel">
        <div className="map-market-list-heading">
          <div>
            <span>좌표가 등록된 시장</span>
            <strong>{mappableMarkets.length}곳</strong>
          </div>
          {markets.length !== mappableMarkets.length && (
            <small>좌표 없음 {markets.length - mappableMarkets.length}곳</small>
          )}
        </div>

        <div className="map-market-list">
          {mappableMarkets.map((market) => {
            const isSelected = market.id === selectedMarketId
            const fullAddress = [market.address, market.detailAddress].filter(Boolean).join(' ')

            return (
              <article
                id={`market-map-item-${market.id}`}
                className={`map-market-item ${isSelected ? 'is-selected' : ''}`}
                key={market.id}
              >
                <button type="button" onClick={() => focusMarket(market)}>
                  <span className="map-market-item-icon" aria-hidden="true">
                    <Store size={18} />
                  </span>
                  <span className="map-market-item-copy">
                    <span className={`status-badge status-${market.status.toLowerCase()}`}>
                      <span aria-hidden="true" />
                      {STATUS_LABELS[market.status]}
                    </span>
                    <strong>{market.name}</strong>
                    <small>{fullAddress}</small>
                  </span>
                  <LocateFixed size={17} aria-hidden="true" />
                </button>
                <Link to={`/markets/${market.id}`}>
                  상세보기
                  <ArrowRight size={14} aria-hidden="true" />
                </Link>
              </article>
            )
          })}
        </div>
      </aside>

      <div className="market-map-canvas-panel">
        <form className="map-address-search" onSubmit={handleAddressSearch}>
          <Search size={19} aria-hidden="true" />
          <label className="sr-only" htmlFor="map-address-input">주소 또는 시장명 검색</label>
          <input
            id="map-address-input"
            type="search"
            value={addressInput}
            onChange={(event) => setAddressInput(event.target.value)}
            placeholder="주소 또는 시장명을 입력해주세요."
            autoComplete="off"
          />
          <button type="submit" disabled={searchFeedback.status === 'searching'}>
            {searchFeedback.status === 'searching'
              ? <LoaderCircle className="spin" size={17} aria-hidden="true" />
              : <Search size={17} aria-hidden="true" />}
            검색
          </button>
        </form>

        {searchFeedback.status !== 'idle' && (
          <div className={`map-search-feedback is-${searchFeedback.status}`} role="status">
            {searchFeedback.status === 'empty' || searchFeedback.status === 'error'
              ? <SearchX size={16} aria-hidden="true" />
              : <MapPin size={16} aria-hidden="true" />}
            <span>{searchFeedback.message}</span>
          </div>
        )}

        <div
          ref={mapContainerRef}
          className="market-explorer-map"
          role="img"
          aria-label="전통시장 위치를 표시한 카카오 지도"
        />

        {mapStatus !== 'ready' && (
          <div className="market-explorer-map-status" role="status">
            {mapStatus === 'loading' && (
              <>
                <LoaderCircle className="spin" size={30} aria-hidden="true" />
                <strong>카카오 지도를 불러오고 있어요.</strong>
              </>
            )}
            {mapStatus === 'missing-key' && (
              <>
                <MapPinned size={30} aria-hidden="true" />
                <strong>카카오 지도 키 설정이 필요합니다.</strong>
                <span>frontend/.env에 VITE_KAKAO_MAP_APP_KEY를 설정해주세요.</span>
              </>
            )}
            {mapStatus === 'error' && (
              <>
                <TriangleAlert size={30} aria-hidden="true" />
                <strong>카카오 지도를 불러오지 못했습니다.</strong>
                <span>JavaScript 키와 등록된 도메인을 확인해주세요.</span>
              </>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
