import { fetchWithCsrf } from './authApi'
import type {
  PaymentConfirmResponse,
  TossPaymentConfirmInput,
} from '../types/payment'

interface ApiErrorBody {
  code?: string
  message?: string
}

export class PaymentApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'PaymentApiError'
  }
}

async function toPaymentApiError(response: Response) {
  let body: ApiErrorBody = {}

  try {
    body = await response.json() as ApiErrorBody
  } catch {
    // JSON 오류 본문이 없으면 화면용 기본 메시지를 사용합니다.
  }

  return new PaymentApiError(
    body.message || '결제 승인에 실패했습니다.',
    response.status,
    body.code,
  )
}

export async function confirmTossPayment(
  input: TossPaymentConfirmInput,
): Promise<PaymentConfirmResponse> {
  const response = await fetchWithCsrf('/payments/toss/confirm', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw await toPaymentApiError(response)
  }

  return response.json() as Promise<PaymentConfirmResponse>
}
