import type {
  AuthUser,
  CsrfTokenResponse,
  LoginInput,
  SignupInput,
} from '../types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiErrorBody {
  code?: string
  message?: string
}

let csrfToken: CsrfTokenResponse | null = null

function readCookie(name: string): string | null {
  const prefix = `${encodeURIComponent(name)}=`
  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(prefix))

  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null
}

export class AuthApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'AuthApiError'
  }
}

async function toAuthApiError(response: Response, fallbackMessage: string) {
  let errorBody: ApiErrorBody = {}

  try {
    errorBody = await response.json() as ApiErrorBody
  } catch {
    // JSON 본문이 없는 응답은 화면에서 이해하기 쉬운 기본 문구로 처리합니다.
  }

  return new AuthApiError(
    errorBody.message || fallbackMessage,
    response.status,
    errorBody.code,
  )
}

async function isInvalidCsrfResponse(response: Response): Promise<boolean> {
  if (response.status !== 403) return false

  try {
    const errorBody = await response.clone().json() as ApiErrorBody
    return errorBody.code === 'INVALID_CSRF_TOKEN'
  } catch {
    return false
  }
}

async function getCsrfToken(forceRefresh = false): Promise<CsrfTokenResponse> {
  if (csrfToken && !forceRefresh) return csrfToken

  const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw await toAuthApiError(response, '보안 토큰을 발급받지 못했습니다.')
  }

  const responseToken = await response.json() as CsrfTokenResponse

  // Spring Security의 SPA CSRF 설정은 응답 본문에는 마스킹된 값을,
  // XSRF-TOKEN 쿠키에는 요청 헤더로 보낼 원본 값을 담습니다.
  csrfToken = {
    ...responseToken,
    token: readCookie('XSRF-TOKEN') || responseToken.token,
  }
  return csrfToken
}

export async function fetchWithCsrf(
  path: string,
  init: RequestInit,
  retryAfterInvalidCsrf = true,
): Promise<Response> {
  const token = await getCsrfToken()
  const headers = new Headers(init.headers)
  headers.set(token.headerName, token.token)

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })

  if (retryAfterInvalidCsrf && await isInvalidCsrfResponse(response)) {
    csrfToken = null
    return fetchWithCsrf(path, init, false)
  }

  return response
}

export async function getCurrentUser(): Promise<AuthUser | null> {
  const response = await fetch(`${API_BASE_URL}/auth/me`, {
    credentials: 'include',
  })

  if (response.status === 401) return null

  if (!response.ok) {
    throw await toAuthApiError(response, '로그인 상태를 확인하지 못했습니다.')
  }

  return response.json() as Promise<AuthUser>
}

export async function signup(input: SignupInput): Promise<AuthUser> {
  const response = await fetchWithCsrf('/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toAuthApiError(response, '회원가입에 실패했습니다.')
  }

  return response.json() as Promise<AuthUser>
}

export async function login(input: LoginInput): Promise<AuthUser> {
  const response = await fetchWithCsrf('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toAuthApiError(response, '이메일 또는 비밀번호를 확인해주세요.')
  }

  csrfToken = null
  return response.json() as Promise<AuthUser>
}

export async function logout(): Promise<void> {
  const response = await fetchWithCsrf('/auth/logout', { method: 'POST' })

  if (!response.ok) {
    throw await toAuthApiError(response, '로그아웃하지 못했습니다.')
  }

  csrfToken = null
}
