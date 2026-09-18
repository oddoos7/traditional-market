import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createOrderDetail } from '../test/orderFixtures'
import { render, screen, userEvent, waitFor } from '../test/render'
import { TossPaymentWidget } from './TossPaymentWidget'

const tossMocks = vi.hoisted(() => ({
  createTossWidgets: vi.fn(),
  getOrCreateTossCustomerKey: vi.fn(() => 'test-customer-key'),
  getTossPaymentsClientKey: vi.fn((): string | null => 'test_gck_client_key'),
}))

vi.mock('../lib/tossPayments', () => tossMocks)

const destroyMethods = vi.fn()
const destroyAgreement = vi.fn()
const widgets = {
  setAmount: vi.fn(),
  renderPaymentMethods: vi.fn(),
  renderAgreement: vi.fn(),
  requestPayment: vi.fn(),
}
const user = {
  id: 1,
  email: 'buyer@example.com',
  name: '구매자',
  role: 'USER' as const,
  createdAt: '2026-09-18T10:00:00',
}

describe('TossPaymentWidget', () => {
  beforeEach(() => {
    Object.values(widgets).forEach((mock) => mock.mockReset())
    destroyMethods.mockReset()
    destroyAgreement.mockReset()
    tossMocks.createTossWidgets.mockReset()
    tossMocks.getOrCreateTossCustomerKey.mockClear()
    tossMocks.getTossPaymentsClientKey.mockReturnValue('test_gck_client_key')
    widgets.setAmount.mockResolvedValue(undefined)
    widgets.renderPaymentMethods.mockResolvedValue({ destroy: destroyMethods })
    widgets.renderAgreement.mockResolvedValue({ destroy: destroyAgreement })
    widgets.requestPayment.mockResolvedValue(undefined)
    tossMocks.createTossWidgets.mockResolvedValue(widgets)
  })

  it('DB 주문 금액으로 위젯을 초기화하고 결제창을 연다', async () => {
    const interaction = userEvent.setup()
    render(<TossPaymentWidget order={createOrderDetail()} user={user} />)

    const payButton = await screen.findByRole('button', { name: '17,000원 결제하기' })
    expect(widgets.setAmount).toHaveBeenCalledWith({ currency: 'KRW', value: 17000 })
    expect(widgets.renderPaymentMethods).toHaveBeenCalledWith({
      selector: '#toss-payment-methods-50',
    })

    await interaction.click(payButton)
    await waitFor(() => expect(widgets.requestPayment).toHaveBeenCalledWith(expect.objectContaining({
      orderId: 'ORD-TEST1234',
      orderName: '새콤달콤 사과 1kg',
      customerEmail: 'buyer@example.com',
      customerName: '구매자',
      successUrl: expect.stringContaining('/payments/toss/success'),
      failUrl: expect.stringContaining('/payments/toss/fail'),
    })))
  })

  it('테스트 클라이언트 키가 없으면 설정 안내를 표시한다', () => {
    tossMocks.getTossPaymentsClientKey.mockReturnValue(null)
    render(<TossPaymentWidget order={createOrderDetail()} user={user} />)

    expect(screen.getByRole('heading', { name: '테스트 결제 키가 필요해요' })).toBeInTheDocument()
    expect(tossMocks.createTossWidgets).not.toHaveBeenCalled()
  })
})
