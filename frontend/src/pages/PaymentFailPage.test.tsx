import { describe, expect, it } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '../test/render'
import { PaymentFailPage } from './PaymentFailPage'

describe('PaymentFailPage', () => {
  it('토스 결제 실패 정보를 안전하게 화면에 표시한다', () => {
    renderWithProviders(
      <Routes>
        <Route path="/payments/toss/fail" element={<PaymentFailPage />} />
      </Routes>,
      {
        initialEntries: [
          '/payments/toss/fail?code=PAY_PROCESS_CANCELED&message=결제를%20취소했습니다&orderId=ORD-TEST1234',
        ],
      },
    )

    expect(screen.getByRole('heading', { name: '결제가 완료되지 않았어요' })).toBeInTheDocument()
    expect(screen.getByText('결제를 취소했습니다')).toBeInTheDocument()
    expect(screen.getByText('PAY_PROCESS_CANCELED')).toBeInTheDocument()
  })
})
