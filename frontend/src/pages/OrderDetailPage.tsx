import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
  LoaderCircle,
  Package,
  RefreshCw,
  ShoppingBag,
  Store,
  XCircle,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelOrder, getOrder, OrderApiError } from '../api/orderApi'
import { useAuth } from '../auth/AuthContext'
import { formatProductPrice } from '../constants/product'
import { ORDER_STATUS_LABELS, formatOrderDate } from './OrdersPage'

export function OrderDetailPage() {
  const { orderId: orderIdParam } = useParams()
  const orderId = Number(orderIdParam)
  const validOrderId = Number.isInteger(orderId) && orderId > 0
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [errorMessage, setErrorMessage] = useState('')
  const queryKey = ['order', user?.id, orderId] as const

  const orderQuery = useQuery({
    queryKey,
    queryFn: () => getOrder(orderId),
    enabled: Boolean(user) && validOrderId,
    retry: false,
  })

  const cancelMutation = useMutation({
    mutationFn: () => cancelOrder(orderId),
    onSuccess: async (order) => {
      queryClient.setQueryData(queryKey, order)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['orders'] }),
        queryClient.invalidateQueries({ queryKey: ['products'] }),
        queryClient.invalidateQueries({ queryKey: ['product'] }),
      ])
      setErrorMessage('')
    },
    onError: (error) => {
      setErrorMessage(error instanceof OrderApiError
        ? error.message
        : '주문을 취소하지 못했습니다. 다시 시도해주세요.')
    },
  })

  useEffect(() => {
    document.title = '주문 상세 | 우리동네 전통시장'
  }, [])

  if (!validOrderId) return <OrderDetailState kind="not-found" />
  if (orderQuery.isLoading) return <OrderDetailState kind="loading" />
  if (orderQuery.isError || !orderQuery.data) {
    const notFound = orderQuery.error instanceof OrderApiError
      && orderQuery.error.status === 404
    return (
      <OrderDetailState
        kind={notFound ? 'not-found' : 'error'}
        onRetry={notFound ? undefined : () => orderQuery.refetch()}
      />
    )
  }

  const order = orderQuery.data
  return (
    <main className="order-detail-main">
      <nav className="order-breadcrumb" aria-label="현재 위치">
        <Link to="/orders"><ArrowLeft size={16} aria-hidden="true" /> 주문 내역</Link>
        <span aria-hidden="true">/</span>
        <span>{order.orderNumber}</span>
      </nav>

      <section className="order-detail-heading" aria-labelledby="order-detail-title">
        <div>
          <span className="eyebrow">Order detail</span>
          <h1 id="order-detail-title">주문 상세</h1>
          <p>{formatOrderDate(order.createdAt)} 주문</p>
        </div>
        <span className={`order-status order-status-${order.status.toLowerCase()}`}>
          {ORDER_STATUS_LABELS[order.status]}
        </span>
      </section>

      <div className="order-detail-layout">
        <section className="order-detail-panel" aria-labelledby="order-items-title">
          <div className="order-detail-store">
            <span><Store size={19} aria-hidden="true" /></span>
            <div>
              <small>{order.marketName}</small>
              <Link to={`/markets/${order.marketId}/stores/${order.storeId}`}>
                {order.storeName}
              </Link>
            </div>
          </div>
          <h2 id="order-items-title">주문 상품</h2>
          <div className="order-product-list">
            {order.items.map((item) => (
              <article className="order-product" key={item.orderItemId}>
                <Link
                  className="order-product-visual"
                  to={`/markets/${order.marketId}/stores/${order.storeId}/products/${item.productId}`}
                >
                  {item.productImageUrl ? (
                    <img src={item.productImageUrl} alt={`${item.productName} 상품`} />
                  ) : (
                    <ShoppingBag size={29} aria-label="상품 이미지 준비 중" />
                  )}
                </Link>
                <div>
                  <Link to={`/markets/${order.marketId}/stores/${order.storeId}/products/${item.productId}`}>
                    {item.productName}
                  </Link>
                  <small>{formatProductPrice(item.unitPrice)} · {item.quantity}개</small>
                </div>
                <strong>{formatProductPrice(item.lineAmount)}</strong>
              </article>
            ))}
          </div>
        </section>

        <aside className="order-payment-panel" aria-labelledby="order-payment-title">
          <h2 id="order-payment-title">결제 정보</h2>
          <dl>
            <div><dt>주문번호</dt><dd>{order.orderNumber}</dd></div>
            <div><dt>상품 수량</dt><dd>{order.totalItemCount.toLocaleString()}개</dd></div>
          </dl>
          <div className="order-payment-total">
            <span>총 결제 금액</span>
            <strong>{formatProductPrice(order.totalAmount)}</strong>
          </div>

          {order.status === 'PENDING_PAYMENT' && (
            <div className="order-action-box is-pending">
              <Clock3 size={21} aria-hidden="true" />
              <div><strong>결제 대기 중</strong><p>현재 상품 재고가 주문 수량만큼 확보되었습니다.</p></div>
            </div>
          )}
          {order.status === 'PAID' && (
            <div className="order-action-box is-paid">
              <CheckCircle2 size={21} aria-hidden="true" />
              <div><strong>결제가 완료됐어요</strong><p>점포에서 주문을 확인하고 있습니다.</p></div>
            </div>
          )}
          {order.status === 'CANCELED' && (
            <div className="order-action-box is-canceled">
              <XCircle size={21} aria-hidden="true" />
              <div><strong>취소된 주문입니다</strong><p>확보했던 상품 재고가 다시 복원되었습니다.</p></div>
            </div>
          )}

          {errorMessage && (
            <p className="order-action-error" role="alert">
              <AlertTriangle size={16} aria-hidden="true" /> {errorMessage}
            </p>
          )}

          {order.status === 'PENDING_PAYMENT' && (
            <>
              <button className="order-pay-button" type="button" disabled>
                테스트 결제 준비 중
              </button>
              <button
                className="order-cancel-button"
                type="button"
                disabled={cancelMutation.isPending}
                onClick={() => {
                  if (window.confirm('주문을 취소하고 확보된 재고를 복원할까요?')) {
                    cancelMutation.mutate()
                  }
                }}
              >
                {cancelMutation.isPending
                  ? <LoaderCircle className="spin" size={16} aria-hidden="true" />
                  : null}
                {cancelMutation.isPending ? '취소 처리 중' : '주문 취소'}
              </button>
              <small>결제 기능은 다음 단계에서 토스페이먼츠 테스트 모드로 연결됩니다.</small>
            </>
          )}
        </aside>
      </div>
    </main>
  )
}

function OrderDetailState({
  kind,
  onRetry,
}: {
  kind: 'loading' | 'error' | 'not-found'
  onRetry?: () => void
}) {
  return (
    <main className="order-detail-main">
      <div className="order-state" role={kind === 'error' ? 'alert' : undefined}>
        {kind === 'loading' && <LoaderCircle className="spin" size={30} aria-hidden="true" />}
        {kind === 'error' && <RefreshCw size={30} aria-hidden="true" />}
        {kind === 'not-found' && <Package size={32} aria-hidden="true" />}
        <h1>
          {kind === 'loading' && '주문 상세를 불러오고 있어요'}
          {kind === 'error' && '주문 상세를 불러오지 못했어요'}
          {kind === 'not-found' && '찾을 수 없는 주문이에요'}
        </h1>
        {kind === 'error' && <button type="button" onClick={onRetry}>다시 불러오기</button>}
        {kind === 'not-found' && <Link to="/orders">주문 내역으로 돌아가기</Link>}
      </div>
    </main>
  )
}
