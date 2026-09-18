import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createContext,
  ReactNode,
  useContext,
  useMemo,
} from 'react'
import { getCurrentUser, login, logout, signup } from '../api/authApi'
import type { AuthUser, LoginInput, SignupInput } from '../types/auth'

const AUTH_QUERY_KEY = ['auth', 'me'] as const
const SESSION_SCOPED_QUERY_NAMES = new Set([
  'favorites',
  'favorite-status',
  'my-store-review',
  'store-reviews',
  'cart',
  'orders',
  'order',
])

interface AuthContextValue {
  user: AuthUser | null
  isCheckingAuth: boolean
  isLoggingIn: boolean
  isSigningUp: boolean
  isLoggingOut: boolean
  loginUser: (input: LoginInput) => Promise<AuthUser>
  signupUser: (input: SignupInput) => Promise<AuthUser>
  logoutUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const currentUserQuery = useQuery({
    queryKey: AUTH_QUERY_KEY,
    queryFn: getCurrentUser,
    retry: false,
    staleTime: 60_000,
  })

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (user) => queryClient.setQueryData(AUTH_QUERY_KEY, user),
  })

  const signupMutation = useMutation({ mutationFn: signup })

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      await queryClient.cancelQueries({
        predicate: (query) => isSessionScopedQuery(query.queryKey),
      })
      queryClient.removeQueries({
        predicate: (query) => isSessionScopedQuery(query.queryKey),
      })
      queryClient.setQueryData(AUTH_QUERY_KEY, null)
    },
  })

  const value = useMemo<AuthContextValue>(() => ({
    user: currentUserQuery.data ?? null,
    isCheckingAuth: currentUserQuery.isLoading,
    isLoggingIn: loginMutation.isPending,
    isSigningUp: signupMutation.isPending,
    isLoggingOut: logoutMutation.isPending,
    loginUser: loginMutation.mutateAsync,
    signupUser: signupMutation.mutateAsync,
    logoutUser: logoutMutation.mutateAsync,
  }), [
    currentUserQuery.data,
    currentUserQuery.isLoading,
    loginMutation.isPending,
    loginMutation.mutateAsync,
    signupMutation.isPending,
    signupMutation.mutateAsync,
    logoutMutation.isPending,
    logoutMutation.mutateAsync,
  ])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서 사용해야 합니다.')
  }

  return context
}

function isSessionScopedQuery(queryKey: readonly unknown[]) {
  const [queryName] = queryKey
  return typeof queryName === 'string' && SESSION_SCOPED_QUERY_NAMES.has(queryName)
}
