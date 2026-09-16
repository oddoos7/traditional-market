const KAKAO_MAP_SDK_ID = 'kakao-map-sdk'

export interface KakaoLatLng {}

export interface KakaoMapControl {}

export interface KakaoControlPosition {}

export interface KakaoMapInstance {
  addControl(control: KakaoMapControl, position: KakaoControlPosition): void
}

export interface KakaoMarkerInstance {
  setMap(map: KakaoMapInstance | null): void
}

export interface KakaoMapsSdk {
  load(callback: () => void): void
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng
  Map: new (
    container: HTMLElement,
    options: { center: KakaoLatLng; level: number },
  ) => KakaoMapInstance
  Marker: new (options: { position: KakaoLatLng }) => KakaoMarkerInstance
  ZoomControl: new () => KakaoMapControl
  ControlPosition: {
    RIGHT: KakaoControlPosition
  }
}

declare global {
  interface Window {
    kakao?: {
      maps: KakaoMapsSdk
    }
  }
}

let sdkPromise: Promise<KakaoMapsSdk> | null = null

function resolveLoadedSdk(resolve: (sdk: KakaoMapsSdk) => void, reject: (error: Error) => void) {
  const maps = window.kakao?.maps

  if (!maps) {
    reject(new Error('카카오 지도 SDK를 불러오지 못했습니다.'))
    return
  }

  maps.load(() => resolve(maps))
}

export function loadKakaoMaps(appKey: string): Promise<KakaoMapsSdk> {
  if (window.kakao?.maps) {
    return new Promise((resolve, reject) => resolveLoadedSdk(resolve, reject))
  }

  if (sdkPromise) return sdkPromise

  const pendingSdk = new Promise<KakaoMapsSdk>((resolve, reject) => {
    const handleLoad = () => resolveLoadedSdk(resolve, reject)
    const handleError = () => reject(new Error('카카오 지도 SDK 요청이 실패했습니다.'))
    const existingScript = document.getElementById(KAKAO_MAP_SDK_ID) as HTMLScriptElement | null

    if (existingScript) {
      existingScript.addEventListener('load', handleLoad, { once: true })
      existingScript.addEventListener('error', handleError, { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = KAKAO_MAP_SDK_ID
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false`
    script.async = true
    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })
    document.head.appendChild(script)
  })

  sdkPromise = pendingSdk.catch((error: unknown) => {
    sdkPromise = null
    throw error
  })

  return sdkPromise
}
