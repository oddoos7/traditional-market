import { AlertTriangle, ReceiptText } from 'lucide-react'
import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'

export function PaymentFailPage() {
  const { search } = useLocation()
  const params = new URLSearchParams(search)
  const code = params.get('code')?.trim()
  const message = params.get('message')?.trim()
  const orderId = params.get('orderId')?.trim()

  useEffect(() => {
    document.title = '결제 실패 | 우리동네 전통시장'
  }, [])

  return (
    <main className="payment-result-main">
      <section className="payment-result-card is-error" role="alert">
        <span className="payment-result-icon">
          <AlertTriangle size={36} aria-hidden="true" />
        </span>
        <span className="eyebrow">Payment failed</span>
        <h1>결제가 완료되지 않았어요</h1>
        <p>{message || '결제가 취소되었거나 처리 중 오류가 발생했습니다.'}</p>
        {(code || orderId) && (
          <dl className="payment-result-summary">
            {orderId && <div><dt>주문번호</dt><dd>{orderId}</dd></div>}
            {code && <div><dt>오류코드</dt><dd>{code}</dd></div>}
          </dl>
        )}
        <div className="payment-result-actions">
          <Link to="/orders">
            <ReceiptText size={16} aria-hidden="true" /> 주문 내역으로
          </Link>
        </div>
      </section>
    </main>
  )
}
