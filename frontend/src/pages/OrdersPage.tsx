import { useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  ClipboardList,
  LoaderCircle,
  PackageOpen,
  RefreshCw,
  Store,
} from 'lucide-react'
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getOrders } from '../api/orderApi'
import { useAuth } from '../auth/AuthContext'
import { formatProductPrice } from '../constants/product'
import type { OrderStatus } from '../types/order'

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAID: '결제 완료',
  CANCELED: '주문 취소',
}

export function OrdersPage() {
  const { user } = useAuth()
  const ordersQuery = useQuery({
    queryKey: ['orders', user?.id],
    queryFn: getOrders,
    enabled: Boolean(user),
    retry: false,
  })

  useEffect(() => {
    document.title = '주문 내역 | 우리동네 전통시장'
  }, [])

  return (
    <main className="orders-main">
      <section className="orders-heading" aria-labelledby="orders-title">
        <span aria-hidden="true"><ClipboardList size={28} /></span>
        <div>
          <span className="eyebrow">My orders</span>
          <h1 id="orders-title">주문 내역</h1>
          <p>주문 상태와 상품 내역을 확인할 수 있어요.</p>
        </div>
      </section>

      {ordersQuery.isLoading && (
        <div className="order-state" aria-busy="true">
          <LoaderCircle className="spin" size={30} aria-hidden="true" />
          <strong>주문 내역을 불러오고 있어요.</strong>
        </div>
      )}

      {ordersQuery.isError && (
        <div className="order-state" role="alert">
          <RefreshCw size={30} aria-hidden="true" />
          <h2>주문 내역을 불러오지 못했어요</h2>
          <button type="button" onClick={() => ordersQuery.refetch()}>다시 불러오기</button>
        </div>
      )}

      {ordersQuery.data?.length === 0 && (
        <div className="order-state">
          <PackageOpen size={35} aria-hidden="true" />
          <h2>아직 주문 내역이 없어요</h2>
          <p>시장 상품을 장바구니에 담아 첫 주문을 만들어보세요.</p>
          <Link to="/">시장 둘러보기</Link>
        </div>
      )}

      {ordersQuery.data && ordersQuery.data.length > 0 && (
        <section className="order-card-list" aria-label="주문 목록">
          {ordersQuery.data.map((order) => (
            <article className="order-card" key={order.id}>
              <div className="order-card-topline">
                <div>
                  <time dateTime={order.createdAt}>{formatOrderDate(order.createdAt)}</time>
                  <small>{order.orderNumber}</small>
                </div>
                <span className={`order-status order-status-${order.status.toLowerCase()}`}>
                  {ORDER_STATUS_LABELS[order.status]}
                </span>
              </div>
              <div className="order-card-body">
                <span className="order-store-icon"><Store size={21} aria-hidden="true" /></span>
                <div>
                  <small>{order.marketName}</small>
                  <strong>{order.storeName}</strong>
                  <p>상품 {order.totalItemCount.toLocaleString()}개</p>
                </div>
                <strong>{formatProductPrice(order.totalAmount)}</strong>
              </div>
              <Link to={`/orders/${order.id}`}>
                주문 상세 보기
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
            </article>
          ))}
        </section>
      )}
    </main>
  )
}

export function formatOrderDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
