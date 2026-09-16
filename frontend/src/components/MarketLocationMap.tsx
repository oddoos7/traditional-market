import { ExternalLink, LoaderCircle, MapPin, Navigation, TriangleAlert } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { loadKakaoMaps, type KakaoMarkerInstance } from '../lib/kakaoMaps'

interface MarketLocationMapProps {
  marketName: string
  address: string
  latitude: number | null
  longitude: number | null
}

type MapStatus = 'loading' | 'ready' | 'missing-key' | 'error'

const KAKAO_MAP_APP_KEY = import.meta.env.VITE_KAKAO_MAP_APP_KEY?.trim()

export function MarketLocationMap({
  marketName,
  address,
  latitude,
  longitude,
}: MarketLocationMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null)
  const hasCoordinates = latitude !== null && longitude !== null
  const [mapStatus, setMapStatus] = useState<MapStatus>(
    KAKAO_MAP_APP_KEY ? 'loading' : 'missing-key',
  )

  useEffect(() => {
    const container = mapContainerRef.current
    if (!container || !hasCoordinates || !KAKAO_MAP_APP_KEY) return

    let cancelled = false
    let marker: KakaoMarkerInstance | null = null
    setMapStatus('loading')

    loadKakaoMaps(KAKAO_MAP_APP_KEY)
      .then((maps) => {
        if (cancelled) return

        const position = new maps.LatLng(latitude, longitude)
        const map = new maps.Map(container, {
          center: position,
          level: 3,
        })

        marker = new maps.Marker({ position })
        marker.setMap(map)
        map.addControl(new maps.ZoomControl(), maps.ControlPosition.RIGHT)
        setMapStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setMapStatus('error')
      })

    return () => {
      cancelled = true
      marker?.setMap(null)
      container.replaceChildren()
    }
  }, [hasCoordinates, latitude, longitude])

  const kakaoPlacePath = hasCoordinates
    ? `${encodeURIComponent(marketName)},${latitude},${longitude}`
    : ''

  return (
    <section className="market-map-card" aria-labelledby="market-map-title">
      <div className="location-panel">
        <div className="location-copy">
          <span className="location-icon">
            <MapPin size={24} aria-hidden="true" />
          </span>
          <div>
            <span className="location-label">시장 위치</span>
            <h3 id="market-map-title">{marketName}</h3>
            <p>{address}</p>
          </div>
        </div>
        <div className="coordinate-box">
          {hasCoordinates ? (
            <>
              <span>위도 {latitude}</span>
              <span>경도 {longitude}</span>
            </>
          ) : (
            <span>위치 좌표를 준비 중입니다.</span>
          )}
        </div>
      </div>

      {hasCoordinates ? (
        <div className="kakao-map-wrap">
          <div
            ref={mapContainerRef}
            className="kakao-map"
            role="img"
            aria-label={`${marketName} 위치를 표시한 카카오 지도`}
          />

          {mapStatus !== 'ready' && (
            <MapStatusOverlay status={mapStatus} />
          )}

          <div className="kakao-map-actions" aria-label="카카오 지도 바로가기">
            <a
              href={`https://map.kakao.com/link/map/${kakaoPlacePath}`}
              target="_blank"
              rel="noreferrer"
            >
              <ExternalLink size={15} aria-hidden="true" />
              크게 보기
            </a>
            <a
              href={`https://map.kakao.com/link/to/${kakaoPlacePath}`}
              target="_blank"
              rel="noreferrer"
            >
              <Navigation size={15} aria-hidden="true" />
              길찾기
            </a>
          </div>
        </div>
      ) : (
        <div className="kakao-map-empty" role="status">
          <MapPin size={28} aria-hidden="true" />
          <strong>지도에 표시할 좌표가 없습니다.</strong>
          <span>관리자 화면에서 시장의 위도와 경도를 등록해주세요.</span>
        </div>
      )}
    </section>
  )
}

function MapStatusOverlay({ status }: { status: Exclude<MapStatus, 'ready'> }) {
  if (status === 'loading') {
    return (
      <div className="kakao-map-status" role="status">
        <LoaderCircle className="spin" size={28} aria-hidden="true" />
        <strong>지도를 불러오는 중입니다.</strong>
      </div>
    )
  }

  const isMissingKey = status === 'missing-key'

  return (
    <div className="kakao-map-status kakao-map-status-error" role="status">
      <TriangleAlert size={28} aria-hidden="true" />
      <strong>{isMissingKey ? '카카오 지도 키 설정이 필요합니다.' : '지도를 불러오지 못했습니다.'}</strong>
      <span>
        {isMissingKey
          ? 'frontend/.env 파일에 VITE_KAKAO_MAP_APP_KEY를 설정해주세요.'
          : '도메인 등록과 JavaScript 키를 확인한 후 새로고침해주세요.'}
      </span>
    </div>
  )
}
