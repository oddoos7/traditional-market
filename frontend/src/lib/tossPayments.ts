const TOSS_PAYMENTS_SDK_URL = 'https://js.tosspayments.com/v2/standard'

export interface TossPaymentRequest {
  method?: string
  orderId: string
  orderName: string
  successUrl: string
  failUrl: string
  customerEmail?: string
  customerName?: string
}

interface TossWidgetRenderResult {
  destroy: () => void
}

export interface TossPaymentWidgets {
  setAmount: (amount: { currency: 'KRW'; value: number }) => Promise<void>
  renderPaymentMethods: (options: {
    selector: string
  }) => Promise<TossWidgetRenderResult>
  renderAgreement: (options: {
    selector: string
  }) => Promise<TossWidgetRenderResult>
  requestPayment: (request: TossPaymentRequest) => Promise<void>
}

interface TossPaymentsInstance {
  widgets: (options: { customerKey: string }) => TossPaymentWidgets
}

type TossPaymentsFactory = (clientKey: string) => TossPaymentsInstance

declare global {
  interface Window {
    TossPayments?: TossPaymentsFactory
  }
}

let sdkPromise: Promise<TossPaymentsFactory> | null = null

export function getTossPaymentsClientKey() {
  const clientKey = import.meta.env.VITE_TOSS_PAYMENTS_CLIENT_KEY?.trim() ?? ''
  return clientKey.startsWith('test_gck_') ? clientKey : null
}

export async function createTossWidgets(customerKey: string) {
  const clientKey = getTossPaymentsClientKey()
  if (!clientKey) {
    throw new Error('토스페이먼츠 테스트 클라이언트 키가 설정되지 않았습니다.')
  }

  const TossPayments = await loadTossPaymentsSdk()
  return TossPayments(clientKey).widgets({ customerKey })
}

export function getOrCreateTossCustomerKey(userId: number) {
  const storageKey = `traditional-market:toss-customer:${userId}`
  const existingKey = window.sessionStorage.getItem(storageKey)
  if (existingKey) return existingKey

  const randomPart = typeof window.crypto.randomUUID === 'function'
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const customerKey = `tm-user-${userId}-${randomPart}`
  window.sessionStorage.setItem(storageKey, customerKey)
  return customerKey
}

function loadTossPaymentsSdk(): Promise<TossPaymentsFactory> {
  if (window.TossPayments) return Promise.resolve(window.TossPayments)
  if (sdkPromise) return sdkPromise

  sdkPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>(
      `script[src="${TOSS_PAYMENTS_SDK_URL}"]`,
    )
    const script = existingScript ?? document.createElement('script')

    const handleLoad = () => {
      if (window.TossPayments) {
        resolve(window.TossPayments)
        return
      }
      script.remove()
      sdkPromise = null
      reject(new Error('토스페이먼츠 SDK를 불러오지 못했습니다.'))
    }
    const handleError = () => {
      script.remove()
      sdkPromise = null
      reject(new Error('토스페이먼츠 SDK를 불러오지 못했습니다.'))
    }

    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })

    if (!existingScript) {
      script.src = TOSS_PAYMENTS_SDK_URL
      script.async = true
      document.head.appendChild(script)
    }
  })

  return sdkPromise
}
