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
  balanceAmount: number
  approvedAt: string
  canceledAt: string | null
  receiptUrl: string | null
}

export interface TossPaymentCancelInput {
  cancelReason: string
}

export interface PaymentCancelResponse {
  paymentId: number
  orderId: number
  orderNumber: string
  paymentKey: string
  status: PaymentStatus
  amount: number
  balanceAmount: number
  canceledAmount: number
  cancelReason: string
  transactionKey: string
  canceledAt: string
}
