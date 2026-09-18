import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  ArrowLeft,
  LoaderCircle,
  Minus,
  PackageOpen,
  Plus,
  RefreshCw,
  ShoppingBag,
  ShoppingCart,
  Store,
  Trash2,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  CartApiError,
  clearCart,
  getCart,
  removeCartItem,
  updateCartItemQuantity,
} from '../api/cartApi'
import { useAuth } from '../auth/AuthContext'
import { formatProductPrice, PRODUCT_STATUS_LABELS } from '../constants/product'
import type { Cart, CartItem } from '../types/cart'

export function CartPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [errorMessage, setErrorMessage] = useState('')
  const cartQueryKey = ['cart', user?.id] as const
  const cartQuery = useQuery({
    queryKey: cartQueryKey,
    queryFn: getCart,
    enabled: Boolean(user),
    retry: false,
  })

  const updateMutation = useMutation({
    mutationFn: ({ cartItemId, quantity }: { cartItemId: number; quantity: number }) => (
      updateCartItemQuantity(cartItemId, quantity)
    ),
    onSuccess: (cart) => {
      queryClient.setQueryData(cartQueryKey, cart)
      setErrorMessage('')
    },
    onError: (error) => setErrorMessage(toCartMessage(error)),
  })

  const removeMutation = useMutation({
    mutationFn: removeCartItem,
    onSuccess: (cart) => {
      queryClient.setQueryData(cartQueryKey, cart)
      setErrorMessage('')
    },
    onError: (error) => setErrorMessage(toCartMessage(error)),
  })

  const clearMutation = useMutation({
    mutationFn: clearCart,
    onSuccess: (cart) => {
      queryClient.setQueryData(cartQueryKey, cart)
      setErrorMessage('')
    },
    onError: (error) => setErrorMessage(toCartMessage(error)),
  })

  useEffect(() => {
    document.title = '장바구니 | 우리동네 전통시장'
  }, [])

  const cart = cartQuery.data
  const isMutating = updateMutation.isPending
    || removeMutation.isPending
    || clearMutation.isPending

  return (
    <main className="cart-main">
      <section className="cart-heading" aria-labelledby="cart-title">
        <span className="cart-heading-icon" aria-hidden="true">
          <ShoppingCart size={27} />
        </span>
        <div>
          <span className="eyebrow">My shopping cart</span>
          <h1 id="cart-title">장바구니</h1>
          <p>구매할 상품의 수량과 현재 재고를 확인해보세요.</p>
        </div>
      </section>

      {cartQuery.isLoading && (
        <div className="cart-state" aria-label="장바구니를 불러오는 중" aria-busy="true">
          <LoaderCircle className="spin" size={29} aria-hidden="true" />
          <strong>장바구니를 불러오고 있어요.</strong>
        </div>
      )}

      {cartQuery.isError && (
        <div className="cart-state" role="alert">
          <RefreshCw size={29} aria-hidden="true" />
          <h2>장바구니를 불러오지 못했어요</h2>
          <p>잠시 후 다시 시도해주세요.</p>
          <button type="button" onClick={() => cartQuery.refetch()}>다시 불러오기</button>
        </div>
      )}

      {cart && cart.items.length === 0 && (
        <div className="cart-state cart-empty-state">
          <PackageOpen size={34} aria-hidden="true" />
          <h2>장바구니가 비어 있어요</h2>
          <p>전통시장 점포의 상품을 둘러보고 마음에 드는 상품을 담아보세요.</p>
          <Link to="/">
            <ArrowLeft size={16} aria-hidden="true" />
            시장 둘러보기
          </Link>
        </div>
      )}

      {cart && cart.items.length > 0 && (
        <div className="cart-layout">
          <section className="cart-items-panel" aria-label="장바구니 상품 목록">
            <div className="cart-store-heading">
              <div>
                <span><Store size={18} aria-hidden="true" /></span>
                <div>
                  <Link to={`/markets/${cart.marketId}`}>{cart.marketName}</Link>
                  <strong>{cart.storeName}</strong>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (window.confirm('장바구니의 모든 상품을 삭제할까요?')) {
                    clearMutation.mutate()
                  }
                }}
                disabled={isMutating}
              >
                <Trash2 size={15} aria-hidden="true" />
                전체 삭제
              </button>
            </div>

            {errorMessage && (
              <p className="cart-error" role="alert">
                <AlertTriangle size={17} aria-hidden="true" />
                {errorMessage}
              </p>
            )}

            <div className="cart-item-list">
              {cart.items.map((item) => (
                <CartItemRow
                  key={item.cartItemId}
                  item={item}
                  cart={cart}
                  isUpdating={updateMutation.isPending
                    && updateMutation.variables?.cartItemId === item.cartItemId}
                  isRemoving={removeMutation.isPending
                    && removeMutation.variables === item.cartItemId}
                  disabled={isMutating}
                  onQuantityChange={(quantity) => {
                    setErrorMessage('')
                    updateMutation.mutate({ cartItemId: item.cartItemId, quantity })
                  }}
                  onRemove={() => {
                    setErrorMessage('')
                    removeMutation.mutate(item.cartItemId)
                  }}
                />
              ))}
            </div>
          </section>

          <aside className="cart-summary" aria-labelledby="cart-summary-title">
            <h2 id="cart-summary-title">결제 예정 금액</h2>
            <dl>
              <div>
                <dt>상품 수량</dt>
                <dd>{cart.totalItemCount.toLocaleString()}개</dd>
              </div>
              <div>
                <dt>상품 금액</dt>
                <dd>{formatProductPrice(cart.totalAmount)}</dd>
              </div>
            </dl>
            <div className="cart-summary-total">
              <span>총 결제 금액</span>
              <strong>{formatProductPrice(cart.totalAmount)}</strong>
            </div>
            {!cart.orderable && (
              <p className="cart-order-warning">
                <AlertTriangle size={16} aria-hidden="true" />
                품절 또는 재고 부족 상품을 삭제하거나 수량을 조정해주세요.
              </p>
            )}
            <button type="button" disabled>
              주문 기능 준비 중
            </button>
            <small>주문 및 재고 처리는 다음 구현 단계에서 연결됩니다.</small>
          </aside>
        </div>
      )}
    </main>
  )
}

function CartItemRow({
  item,
  cart,
  isUpdating,
  isRemoving,
  disabled,
  onQuantityChange,
  onRemove,
}: {
  item: CartItem
  cart: Cart
  isUpdating: boolean
  isRemoving: boolean
  disabled: boolean
  onQuantityChange: (quantity: number) => void
  onRemove: () => void
}) {
  const productPath = `/markets/${cart.marketId}/stores/${cart.storeId}/products/${item.productId}`
  const maxQuantity = Math.min(item.stockQuantity, 99)

  return (
    <article className={`cart-item ${item.available ? '' : 'is-unavailable'}`}>
      <Link className="cart-item-visual" to={productPath}>
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={`${item.name} 상품`} />
        ) : (
          <span aria-label="상품 이미지 준비 중">
            <ShoppingBag size={31} strokeWidth={1.6} aria-hidden="true" />
          </span>
        )}
      </Link>
      <div className="cart-item-copy">
        <div>
          <span className={`product-status product-status-${item.productStatus.toLowerCase()}`}>
            {item.available ? PRODUCT_STATUS_LABELS[item.productStatus] : '구매 불가'}
          </span>
          <Link to={productPath}>{item.name}</Link>
          <small>개당 {formatProductPrice(item.unitPrice)}</small>
        </div>
        {!item.available && (
          <div className="cart-stock-warning">
            <p>
              {item.productStatus !== 'ON_SALE'
                ? '판매 상태가 변경된 상품입니다.'
                : `현재 재고는 ${item.stockQuantity.toLocaleString()}개입니다.`}
            </p>
            {item.productStatus === 'ON_SALE' && item.stockQuantity > 0 && (
              <button
                type="button"
                onClick={() => onQuantityChange(maxQuantity)}
                disabled={disabled}
              >
                재고에 맞추기
              </button>
            )}
          </div>
        )}
      </div>
      <div className="cart-item-quantity">
        <span>수량</span>
        <div className="cart-quantity-control">
          <button
            type="button"
            onClick={() => onQuantityChange(item.quantity - 1)}
            disabled={disabled || !item.available || item.quantity <= 1}
            aria-label={`${item.name} 수량 줄이기`}
          >
            <Minus size={14} aria-hidden="true" />
          </button>
          <strong>{isUpdating ? <LoaderCircle className="spin" size={15} /> : item.quantity}</strong>
          <button
            type="button"
            onClick={() => onQuantityChange(item.quantity + 1)}
            disabled={disabled || !item.available || item.quantity >= maxQuantity}
            aria-label={`${item.name} 수량 늘리기`}
          >
            <Plus size={14} aria-hidden="true" />
          </button>
        </div>
      </div>
      <strong className="cart-item-amount">{formatProductPrice(item.lineAmount)}</strong>
      <button
        className="cart-item-remove"
        type="button"
        onClick={onRemove}
        disabled={disabled}
        aria-label={`${item.name} 장바구니에서 삭제`}
      >
        {isRemoving
          ? <LoaderCircle className="spin" size={17} aria-hidden="true" />
          : <Trash2 size={17} aria-hidden="true" />}
      </button>
    </article>
  )
}

function toCartMessage(error: unknown) {
  if (!(error instanceof CartApiError)) {
    return '장바구니를 변경하지 못했습니다. 다시 시도해주세요.'
  }

  switch (error.code) {
    case 'CART_QUANTITY_EXCEEDED':
      return '현재 재고보다 많은 수량으로 변경할 수 없습니다.'
    case 'PRODUCT_NOT_AVAILABLE':
      return '현재 구매할 수 없는 상품입니다.'
    case 'CART_ITEM_NOT_FOUND':
      return '장바구니에서 해당 상품을 찾을 수 없습니다. 새로고침해주세요.'
    default:
      return error.message
  }
}
