import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, LoaderCircle, Minus, Plus, ShoppingCart } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { addCartItem, CartApiError } from '../api/cartApi'
import { useAuth } from '../auth/AuthContext'
import type { Cart } from '../types/cart'
import type { ProductDetail } from '../types/product'

export function AddToCartPanel({ product }: { product: ProductDetail }) {
  const { user } = useAuth()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const maxQuantity = Math.min(product.stockQuantity, 99)
  const canAdd = product.status === 'ON_SALE' && maxQuantity > 0

  const addMutation = useMutation({
    mutationFn: () => addCartItem({ productId: product.id, quantity }),
    onSuccess: (cart: Cart) => {
      queryClient.setQueryData(['cart', user?.id], cart)
      setErrorMessage('')
      setMessage(`${product.name} ${quantity}개를 장바구니에 담았습니다.`)
    },
    onError: (error) => {
      setMessage('')
      setErrorMessage(getCartErrorMessage(error))
    },
  })

  if (!user) {
    return (
      <div className="add-cart-panel add-cart-login">
        <ShoppingCart size={21} aria-hidden="true" />
        <p>로그인하면 상품을 장바구니에 담을 수 있습니다.</p>
        <Link
          to="/login"
          state={{
            from: location.pathname,
            message: '장바구니는 로그인 후 이용할 수 있습니다.',
          }}
        >
          로그인하기
        </Link>
      </div>
    )
  }

  return (
    <div className="add-cart-panel">
      <div className="add-cart-controls">
        <div>
          <span>구매 수량</span>
          <small>한 상품당 최대 99개</small>
        </div>
        <div className="cart-quantity-control" aria-label="장바구니에 담을 수량">
          <button
            type="button"
            onClick={() => setQuantity((current) => Math.max(1, current - 1))}
            disabled={!canAdd || quantity <= 1 || addMutation.isPending}
            aria-label="수량 줄이기"
          >
            <Minus size={15} aria-hidden="true" />
          </button>
          <strong>{quantity}</strong>
          <button
            type="button"
            onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))}
            disabled={!canAdd || quantity >= maxQuantity || addMutation.isPending}
            aria-label="수량 늘리기"
          >
            <Plus size={15} aria-hidden="true" />
          </button>
        </div>
      </div>
      <button
        className="add-cart-button"
        type="button"
        onClick={() => {
          setMessage('')
          setErrorMessage('')
          addMutation.mutate()
        }}
        disabled={!canAdd || addMutation.isPending}
      >
        {addMutation.isPending
          ? <LoaderCircle className="spin" size={18} aria-hidden="true" />
          : <ShoppingCart size={18} aria-hidden="true" />}
        {addMutation.isPending ? '담는 중...' : canAdd ? '장바구니 담기' : '현재 구매 불가'}
      </button>
      {message && (
        <div className="add-cart-feedback success" role="status">
          <CheckCircle2 size={17} aria-hidden="true" />
          <span>{message}</span>
          <Link to="/cart">장바구니 보기</Link>
        </div>
      )}
      {errorMessage && (
        <div className="add-cart-feedback error" role="alert">
          <span>{errorMessage}</span>
          <Link to="/cart">장바구니 확인</Link>
        </div>
      )}
    </div>
  )
}

function getCartErrorMessage(error: unknown) {
  if (!(error instanceof CartApiError)) {
    return '상품을 장바구니에 담지 못했습니다. 다시 시도해주세요.'
  }

  switch (error.code) {
    case 'CART_STORE_MISMATCH':
      return '다른 점포 상품이 담겨 있어요. 기존 장바구니를 비운 뒤 다시 시도해주세요.'
    case 'CART_QUANTITY_EXCEEDED':
      return '현재 재고보다 많은 수량은 담을 수 없습니다.'
    case 'PRODUCT_NOT_AVAILABLE':
      return '현재 판매하지 않는 상품입니다.'
    default:
      return error.message
  }
}
