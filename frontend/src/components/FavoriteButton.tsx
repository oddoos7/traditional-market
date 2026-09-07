import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Heart, LoaderCircle } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  addFavorite,
  FavoriteApiError,
  getFavoriteStatus,
  removeFavorite,
} from '../api/favoriteApi'
import { useAuth } from '../auth/AuthContext'
import type { FavoriteStatus } from '../types/favorite'

export function FavoriteButton({ storeId }: { storeId: number }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [errorMessage, setErrorMessage] = useState('')
  const queryKey = ['favorite-status', user?.id, storeId] as const

  const statusQuery = useQuery({
    queryKey,
    queryFn: () => getFavoriteStatus(storeId),
    enabled: Boolean(user),
    retry: false,
  })

  const favoriteMutation = useMutation({
    mutationFn: async (isFavorite: boolean): Promise<FavoriteStatus> => {
      if (isFavorite) {
        await removeFavorite(storeId)
        return { favorite: false }
      }

      return addFavorite(storeId)
    },
    onSuccess: (status) => {
      queryClient.setQueryData<FavoriteStatus>(queryKey, status)
      queryClient.invalidateQueries({ queryKey: ['favorites', user?.id] })
    },
    onError: (error) => {
      setErrorMessage(
        error instanceof FavoriteApiError
          ? error.message
          : '찜 상태를 변경하지 못했습니다. 다시 시도해주세요.',
      )
    },
  })

  const isFavorite = statusQuery.data?.favorite ?? false
  const isPending = statusQuery.isLoading || favoriteMutation.isPending

  const handleClick = () => {
    setErrorMessage('')

    if (!user) {
      navigate('/login', {
        state: {
          from: location.pathname,
          message: '점포를 찜하려면 먼저 로그인해주세요.',
        },
      })
      return
    }

    if (statusQuery.isError) {
      statusQuery.refetch()
      return
    }

    favoriteMutation.mutate(isFavorite)
  }

  return (
    <div className="favorite-action-wrap">
      <button
        className={`favorite-button ${isFavorite ? 'is-favorite' : ''}`}
        type="button"
        onClick={handleClick}
        disabled={isPending}
        aria-pressed={isFavorite}
      >
        {isPending ? (
          <LoaderCircle className="spin" size={18} aria-hidden="true" />
        ) : (
          <Heart size={18} fill={isFavorite ? 'currentColor' : 'none'} aria-hidden="true" />
        )}
        {statusQuery.isError
          ? '찜 상태 다시 확인'
          : isPending
            ? '처리 중...'
            : isFavorite ? '찜 해제' : '찜하기'}
      </button>
      {errorMessage && <span className="favorite-error" role="alert">{errorMessage}</span>}
    </div>
  )
}
