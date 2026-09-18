export type PaymentStatus =
  | 'READY'
  | 'IN_PROGRESS'
  | 'WAITING_FOR_DEPOSIT'
  | 'DONE'
  | 'CANCELED'
  | 'PARTIAL_CANCELED'
  | 'ABORTED'
  | 'EXPIRED'

export interface TossPaymentConfirmInput {
  paymentKey: string
  orderId: string
  amount: number
}

export interface PaymentConfirmResponse {
  paymentId: number
  orderId: number
  orderNumber: string
  paymentKey: string
  status: PaymentStatus
  method: string
  amount: number
  approvedAt: string
  receiptUrl: string | null
}
