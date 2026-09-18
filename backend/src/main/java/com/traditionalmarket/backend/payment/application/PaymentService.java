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
import com.traditionalmarket.backend.payment.domain.PaymentCancellation;
import com.traditionalmarket.backend.payment.domain.PaymentStatus;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentCancelRequest;
import com.traditionalmarket.backend.payment.dto.request.TossPaymentConfirmRequest;
import com.traditionalmarket.backend.payment.dto.response.PaymentCancelResponse;
import com.traditionalmarket.backend.payment.dto.response.PaymentConfirmResponse;
import com.traditionalmarket.backend.payment.exception.PaymentAmountMismatchException;
import com.traditionalmarket.backend.payment.exception.PaymentCancellationNotAllowedException;
import com.traditionalmarket.backend.payment.exception.PaymentNotFoundException;
import com.traditionalmarket.backend.payment.exception.PaymentOrderNotPayableException;
import com.traditionalmarket.backend.payment.exception.PaymentVerificationException;
import com.traditionalmarket.backend.payment.infrastructure.PaymentIdempotencyKeys;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentsClient;
import com.traditionalmarket.backend.payment.repository.PaymentCancellationRepository;
import com.traditionalmarket.backend.payment.repository.PaymentRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCancellationRepository paymentCancellationRepository;
    private final UserAccountRepository userAccountRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentStateSynchronizer paymentStateSynchronizer;

    @Transactional
    public PaymentConfirmResponse confirmTossPayment(
            String email,
            TossPaymentConfirmRequest request
    ) {
        UserAccount user = getUser(email);
        PurchaseOrder order = purchaseOrderRepository
                .findByOrderNumberAndUserIdForUpdate(request.orderId(), user.getId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));
        long expectedAmount = toKrwAmount(order.getTotalAmount());
        if (request.amount() != expectedAmount) {
            throw new PaymentAmountMismatchException();
        }

        Payment existingPayment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElse(null);
        if (existingPayment != null) {
            validateSameConfirmation(existingPayment, request, expectedAmount);
            return PaymentConfirmResponse.from(existingPayment);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new PaymentOrderNotPayableException(order.getId());
        }

        String idempotencyKey = PaymentIdempotencyKeys.confirmation(
                order.getOrderNumber(),
                request.paymentKey()
        );
        TossPaymentSnapshot snapshot = tossPaymentsClient.confirm(
                request.paymentKey(),
                order.getOrderNumber(),
                expectedAmount,
                idempotencyKey
        );
        if (!request.paymentKey().equals(snapshot.paymentKey())
                || !"DONE".equals(snapshot.status())
                || snapshot.approvedAt() == null) {
            throw new PaymentVerificationException();
        }

        PaymentSyncResult syncResult = paymentStateSynchronizer.synchronize(
                order,
                null,
                snapshot,
                idempotencyKey,
                null
        );
        return PaymentConfirmResponse.from(syncResult.payment());
    }

    @Transactional
    public PaymentCancelResponse cancelTossPayment(
            String email,
            Long orderId,
            TossPaymentCancelRequest request
    ) {
        UserAccount user = getUser(email);
        PurchaseOrder order = purchaseOrderRepository
                .findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CANCELED
                && payment.getStatus() == PaymentStatus.CANCELED) {
            return PaymentCancelResponse.from(payment, getLatestCancellation(payment));
        }
        if (order.getStatus() != OrderStatus.PAID
                || payment.getStatus() != PaymentStatus.DONE) {
            throw new PaymentCancellationNotAllowedException(orderId);
        }

        String idempotencyKey = PaymentIdempotencyKeys.fullCancellation(payment.getPaymentKey());
        TossPaymentSnapshot snapshot = tossPaymentsClient.cancel(
                payment.getPaymentKey(),
                request.cancelReason().trim(),
                idempotencyKey
        );
        if (!"CANCELED".equals(snapshot.status())) {
            throw new PaymentVerificationException();
        }

        PaymentSyncResult syncResult = paymentStateSynchronizer.synchronize(
                order,
                payment,
                snapshot,
                payment.getConfirmationIdempotencyKey(),
                idempotencyKey
        );
        PaymentCancellation cancellation = syncResult.latestCancellation() == null
                ? getLatestCancellation(payment)
                : syncResult.latestCancellation();
        return PaymentCancelResponse.from(syncResult.payment(), cancellation);
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private long toKrwAmount(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentAmountMismatchException();
        }
    }

    private void validateSameConfirmation(
            Payment payment,
            TossPaymentConfirmRequest request,
            long expectedAmount
    ) {
        if (!payment.getPaymentKey().equals(request.paymentKey())
                || payment.getAmount().compareTo(BigDecimal.valueOf(expectedAmount)) != 0) {
            throw new PaymentOrderNotPayableException(payment.getOrder().getId());
        }
    }

    private PaymentCancellation getLatestCancellation(Payment payment) {
        return paymentCancellationRepository
                .findFirstByPaymentIdOrderByCanceledAtDescIdDesc(payment.getId())
                .orElseThrow(PaymentVerificationException::new);
    }
}
