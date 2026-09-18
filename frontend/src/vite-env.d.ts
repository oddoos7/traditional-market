/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_KAKAO_MAP_APP_KEY?: string
  readonly VITE_TOSS_PAYMENTS_CLIENT_KEY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
