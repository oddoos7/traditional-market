import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  LoaderCircle,
  ReceiptText,
} from 'lucide-react'
import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { confirmTossPayment, PaymentApiError } from '../api/paymentApi'
import { formatProductPrice } from '../constants/product'

export function PaymentSuccessPage() {
  const { search } = useLocation()
  const queryClient = useQueryClient()
  const hasRequested = useRef(false)
  const callback = parseSuccessCallback(search)
  const confirmMutation = useMutation({
    mutationFn: confirmTossPayment,
    onSuccess: async (payment) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['orders'] }),
        queryClient.invalidateQueries({ queryKey: ['order'] }),
        queryClient.invalidateQueries({ queryKey: ['products'] }),
        queryClient.invalidateQueries({ queryKey: ['product'] }),
      ])
      queryClient.setQueryData(['payment-confirm', payment.paymentKey], payment)
    },
  })

  useEffect(() => {
    document.title = '결제 결과 | 우리동네 전통시장'
    if (!callback || hasRequested.current) return

    hasRequested.current = true
    confirmMutation.mutate(callback)
  }, [callback, confirmMutation])

  if (!callback) {
    return (
      <PaymentResultLayout kind="error" title="결제 정보를 확인할 수 없어요">
        <p>결제 성공 주소의 필수 값이 없거나 올바르지 않습니다.</p>
        <Link to="/orders">주문 내역으로</Link>
      </PaymentResultLayout>
    )
  }

  if (confirmMutation.isIdle || confirmMutation.isPending) {
    return (
      <PaymentResultLayout kind="loading" title="결제를 승인하고 있어요">
        <p>창을 닫거나 새로고침하지 말고 잠시 기다려주세요.</p>
      </PaymentResultLayout>
    )
  }

  if (confirmMutation.isError) {
    const message = confirmMutation.error instanceof PaymentApiError
      ? confirmMutation.error.message
      : '결제 승인 중 오류가 발생했습니다.'

    return (
      <PaymentResultLayout kind="error" title="결제를 완료하지 못했어요">
        <p>{message}</p>
        <div className="payment-result-actions">
          <button type="button" onClick={() => {
            hasRequested.current = true
            confirmMutation.mutate(callback)
          }}>
            승인 다시 시도
          </button>
          <Link to="/orders">주문 내역으로</Link>
        </div>
      </PaymentResultLayout>
    )
  }

  const payment = confirmMutation.data
  return (
    <PaymentResultLayout kind="success" title="테스트 결제가 완료됐어요">
      <dl className="payment-result-summary">
        <div><dt>주문번호</dt><dd>{payment.orderNumber}</dd></div>
        <div><dt>결제수단</dt><dd>{payment.method}</dd></div>
        <div><dt>결제금액</dt><dd>{formatProductPrice(payment.amount)}</dd></div>
      </dl>
      <div className="payment-result-actions">
        <Link to={`/orders/${payment.orderId}`}>
          <ReceiptText size={16} aria-hidden="true" /> 주문 상세 보기
        </Link>
        {payment.receiptUrl && (
          <a href={payment.receiptUrl} target="_blank" rel="noreferrer">
            영수증 보기 <ExternalLink size={14} aria-hidden="true" />
          </a>
        )}
      </div>
    </PaymentResultLayout>
  )
}

function PaymentResultLayout({
  kind,
  title,
  children,
}: {
  kind: 'loading' | 'success' | 'error'
  title: string
  children: ReactNode
}) {
  return (
    <main className="payment-result-main">
      <section
        className={`payment-result-card is-${kind}`}
        role={kind === 'error' ? 'alert' : undefined}
        aria-busy={kind === 'loading'}
      >
        <span className="payment-result-icon">
          {kind === 'loading' && <LoaderCircle className="spin" size={34} aria-hidden="true" />}
          {kind === 'success' && <CheckCircle2 size={36} aria-hidden="true" />}
          {kind === 'error' && <AlertTriangle size={36} aria-hidden="true" />}
        </span>
        <span className="eyebrow">Payment result</span>
        <h1>{title}</h1>
        {children}
      </section>
    </main>
  )
}

function parseSuccessCallback(search: string) {
  const params = new URLSearchParams(search)
  const paymentKey = params.get('paymentKey')?.trim() ?? ''
  const orderId = params.get('orderId')?.trim() ?? ''
  const amountText = params.get('amount')?.trim() ?? ''
  const amount = Number(amountText)

  if (
    paymentKey.length === 0
    || paymentKey.length > 200
    || !/^[A-Za-z0-9_-]{6,64}$/.test(orderId)
    || !/^\d+$/.test(amountText)
    || !Number.isSafeInteger(amount)
    || amount <= 0
  ) {
    return null
  }

  return { paymentKey, orderId, amount }
}
