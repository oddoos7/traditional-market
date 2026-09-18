import { AlertTriangle, CreditCard, LoaderCircle, ShieldCheck } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { AuthUser } from '../types/auth'
import type { OrderDetail } from '../types/order'
import {
  createTossWidgets,
  getOrCreateTossCustomerKey,
  getTossPaymentsClientKey,
  type TossPaymentWidgets,
} from '../lib/tossPayments'

interface TossPaymentWidgetProps {
  order: OrderDetail
  user: AuthUser
}

export function TossPaymentWidget({ order, user }: TossPaymentWidgetProps) {
  const widgetsRef = useRef<TossPaymentWidgets | null>(null)
  const [isReady, setIsReady] = useState(false)
  const [isRequesting, setIsRequesting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const selectors = useMemo(() => ({
    methods: `#toss-payment-methods-${order.id}`,
    agreement: `#toss-payment-agreement-${order.id}`,
  }), [order.id])
  const hasClientKey = Boolean(getTossPaymentsClientKey())
  const hasValidAmount = Number.isSafeInteger(order.totalAmount) && order.totalAmount > 0

  useEffect(() => {
    if (!hasClientKey || !hasValidAmount) return

    let active = true
    let destroyPaymentMethods: (() => void) | undefined
    let destroyAgreement: (() => void) | undefined

    const initialize = async () => {
      try {
        const widgets = await createTossWidgets(getOrCreateTossCustomerKey(user.id))
        await widgets.setAmount({ currency: 'KRW', value: order.totalAmount })
        const paymentMethods = await widgets.renderPaymentMethods({ selector: selectors.methods })
        destroyPaymentMethods = paymentMethods.destroy
        const agreement = await widgets.renderAgreement({ selector: selectors.agreement })
        destroyAgreement = agreement.destroy

        if (!active) {
          paymentMethods.destroy()
          agreement.destroy()
          destroyPaymentMethods = undefined
          destroyAgreement = undefined
          return
        }

        widgetsRef.current = widgets
        setIsReady(true)
      } catch (error) {
        destroyPaymentMethods?.()
        destroyAgreement?.()
        destroyPaymentMethods = undefined
        destroyAgreement = undefined
        if (active) {
          setErrorMessage(toPaymentWindowMessage(error))
        }
      }
    }

    void initialize()
    return () => {
      active = false
      widgetsRef.current = null
      destroyPaymentMethods?.()
      destroyAgreement?.()
    }
  }, [hasClientKey, hasValidAmount, order.totalAmount, selectors, user.id])

  const handlePayment = async () => {
    if (!widgetsRef.current || isRequesting) return

    const firstItem = order.items[0]
    if (!firstItem) {
      setErrorMessage('주문 상품 정보를 확인하지 못했습니다.')
      return
    }

    setIsRequesting(true)
    setErrorMessage('')
    try {
      const orderName = order.items.length === 1
        ? firstItem.productName
        : `${firstItem.productName} 외 ${order.items.length - 1}건`

      await widgetsRef.current.requestPayment({
        orderId: order.orderNumber,
        orderName,
        successUrl: `${window.location.origin}/payments/toss/success`,
        failUrl: `${window.location.origin}/payments/toss/fail`,
        customerEmail: user.email,
        customerName: user.name,
      })
    } catch (error) {
      setErrorMessage(toPaymentWindowMessage(error))
      setIsRequesting(false)
    }
  }

  if (!hasClientKey) {
    return (
      <section className="toss-payment-widget toss-payment-config" aria-labelledby="toss-title">
        <AlertTriangle size={23} aria-hidden="true" />
        <div>
          <h2 id="toss-title">테스트 결제 키가 필요해요</h2>
          <p><code>frontend/.env.local</code>에 테스트 클라이언트 키를 설정해주세요.</p>
        </div>
      </section>
    )
  }

  if (!hasValidAmount) {
    return (
      <p className="toss-payment-error" role="alert">
        <AlertTriangle size={17} aria-hidden="true" /> 결제 금액을 확인해주세요.
      </p>
    )
  }

  return (
    <section className="toss-payment-widget" aria-labelledby="toss-title">
      <div className="toss-payment-heading">
        <span><CreditCard size={21} aria-hidden="true" /></span>
        <div>
          <h2 id="toss-title">결제수단 선택</h2>
          <p>토스페이먼츠 테스트 모드로 진행됩니다.</p>
        </div>
      </div>

      {!isReady && !errorMessage && (
        <div className="toss-payment-loading" aria-busy="true">
          <LoaderCircle className="spin" size={21} aria-hidden="true" />
          결제수단을 불러오고 있어요.
        </div>
      )}

      <div id={selectors.methods.slice(1)} className="toss-payment-methods" />
      <div id={selectors.agreement.slice(1)} className="toss-payment-agreement" />

      {errorMessage && (
        <p className="toss-payment-error" role="alert">
          <AlertTriangle size={17} aria-hidden="true" /> {errorMessage}
        </p>
      )}

      <button
        className="toss-payment-button"
        type="button"
        disabled={!isReady || isRequesting}
        onClick={handlePayment}
      >
        {isRequesting
          ? <LoaderCircle className="spin" size={18} aria-hidden="true" />
          : <ShieldCheck size={18} aria-hidden="true" />}
        {isRequesting ? '결제창 여는 중' : `${order.totalAmount.toLocaleString()}원 결제하기`}
      </button>
      <small>실제 금액은 청구되지 않는 테스트 결제입니다.</small>
    </section>
  )
}

function toPaymentWindowMessage(error: unknown) {
  if (isTossError(error) && error.code === 'USER_CANCEL') {
    return '결제창을 닫았습니다. 원할 때 다시 시도할 수 있어요.'
  }
  if (isTossError(error) && error.message) return error.message
  if (error instanceof Error) return error.message
  return '결제창을 열지 못했습니다. 잠시 후 다시 시도해주세요.'
}

function isTossError(error: unknown): error is { code?: string; message?: string } {
  return typeof error === 'object' && error !== null
}
