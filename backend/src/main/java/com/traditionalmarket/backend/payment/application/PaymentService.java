package com.traditionalmarket.backend.payment.application;

import java.math.BigDecimal;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.order.domain.OrderStatus;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;
import com.traditionalmarket.backend.order.exception.OrderNotFoundException;
import com.traditionalmarket.backend.order.repository.PurchaseOrderRepository;
import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentConfirmRequest;
import com.traditionalmarket.backend.payment.dto.response.PaymentConfirmResponse;
import com.traditionalmarket.backend.payment.exception.PaymentAmountMismatchException;
import com.traditionalmarket.backend.payment.exception.PaymentOrderNotPayableException;
import com.traditionalmarket.backend.payment.exception.PaymentVerificationException;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentApproval;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;
import com.traditionalmarket.backend.payment.repository.PaymentRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userAccountRepository;
    private final TossPaymentsClient tossPaymentsClient;

    @Transactional
    public PaymentConfirmResponse confirmTossPayment(
            String email,
            TossPaymentConfirmRequest request
    ) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
        PurchaseOrder order = purchaseOrderRepository
                .findByOrderNumberAndUserIdForUpdate(request.orderId(), user.getId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                || paymentRepository.existsByOrderId(order.getId())) {
            throw new PaymentOrderNotPayableException(order.getId());
        }

        long expectedAmount = toKrwAmount(order.getTotalAmount());
        if (request.amount() != expectedAmount) {
            throw new PaymentAmountMismatchException();
        }

        TossPaymentApproval approval = tossPaymentsClient.confirm(
                request.paymentKey(),
                order.getOrderNumber(),
                expectedAmount
        );
        validateApproval(order, request.paymentKey(), expectedAmount, approval);

        var approvedAt = approval.approvedAt().toLocalDateTime();
        order.markPaid(approvedAt);
        Payment payment = Payment.approved(
                order,
                approval.paymentKey(),
                approval.method(),
                BigDecimal.valueOf(approval.totalAmount()),
                approvedAt,
                approval.receiptUrl()
        );
        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        return PaymentConfirmResponse.from(savedPayment);
    }

    private long toKrwAmount(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentAmountMismatchException();
        }
    }

    private void validateApproval(
            PurchaseOrder order,
            String paymentKey,
            long expectedAmount,
            TossPaymentApproval approval
    ) {
        if (approval.approvedAt() == null
                || !"DONE".equals(approval.status())
                || !paymentKey.equals(approval.paymentKey())
                || !order.getOrderNumber().equals(approval.orderId())
                || expectedAmount != approval.totalAmount()) {
            throw new PaymentVerificationException();
        }
    }
}
