export type UserRole = 'USER' | 'ADMIN'

export interface AuthUser {
  id: number
  email: string
  name: string
  role: UserRole
  createdAt: string
}

export interface LoginInput {
  email: string
  password: string
}

export interface SignupInput extends LoginInput {
  name: string
}

export interface CsrfTokenResponse {
  headerName: string
  parameterName: string
  token: string
}
