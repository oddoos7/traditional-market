package com.traditionalmarket.backend.payment.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.traditionalmarket.backend.order.domain.OrderStatus;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;
import com.traditionalmarket.backend.payment.domain.Payment;
import com.traditionalmarket.backend.payment.domain.PaymentCancellation;
import com.traditionalmarket.backend.payment.domain.PaymentStatus;
import com.traditionalmarket.backend.payment.exception.PaymentVerificationException;
import com.traditionalmarket.backend.payment.infrastructure.PaymentIdempotencyKeys;
import com.traditionalmarket.backend.payment.infrastructure.TossCancellationSnapshot;
import com.traditionalmarket.backend.payment.infrastructure.TossPaymentSnapshot;
import com.traditionalmarket.backend.payment.repository.PaymentCancellationRepository;
import com.traditionalmarket.backend.payment.repository.PaymentRepository;
import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentStateSynchronizer {

    private final PaymentRepository paymentRepository;
    private final PaymentCancellationRepository paymentCancellationRepository;
    private final ProductRepository productRepository;

    public PaymentSyncResult synchronize(
            PurchaseOrder order,
            Payment existingPayment,
            TossPaymentSnapshot snapshot,
            String confirmationIdempotencyKey,
            String cancellationIdempotencyKey
    ) {
        validateSnapshot(order, existingPayment, snapshot);
        PaymentStatus providerStatus = parseStatus(snapshot.status());
        if (!isPersistedStatus(providerStatus)) {
            return new PaymentSyncResult(existingPayment, null, "NO_CHANGE");
        }

        boolean changed = false;
        Payment payment = existingPayment;
        if (payment == null) {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                    || snapshot.approvedAt() == null) {
                throw new PaymentVerificationException();
            }
            String idempotencyKey = confirmationIdempotencyKey == null
                    ? PaymentIdempotencyKeys.confirmation(order.getOrderNumber(), snapshot.paymentKey())
                    : confirmationIdempotencyKey;
            LocalDateTime approvedAt = snapshot.approvedAt().toLocalDateTime();
            order.markPaid(approvedAt);
            payment = paymentRepository.saveAndFlush(Payment.approved(
                    order,
                    snapshot.paymentKey(),
                    idempotencyKey,
                    snapshot.method(),
                    BigDecimal.valueOf(snapshot.totalAmount()),
                    approvedAt,
                    snapshot.receiptUrl()
            ));
            changed = true;
        } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.markPaid(payment.getApprovedAt());
            changed = true;
        }

        PaymentCancellation latestCancellation = synchronizeCancellations(
                payment,
                snapshot,
                cancellationIdempotencyKey
        );
        if (latestCancellation != null) {
            changed = true;
        } else {
            latestCancellation = paymentCancellationRepository
                    .findFirstByPaymentIdOrderByCanceledAtDescIdDesc(payment.getId())
                    .orElse(null);
        }

        if (providerStatus == PaymentStatus.DONE) {
            if (order.getStatus() == OrderStatus.CANCELED
                    || payment.getStatus() == PaymentStatus.CANCELED) {
                throw new PaymentVerificationException();
            }
            if (payment.getStatus() != PaymentStatus.DONE
                    || payment.getBalanceAmount().compareTo(payment.getAmount()) != 0) {
                payment.synchronizeStatus(PaymentStatus.DONE, payment.getAmount(), null);
                changed = true;
            }
        } else {
            LocalDateTime canceledAt = requireLatestCancellationTime(snapshot.cancellations());
            BigDecimal balanceAmount = BigDecimal.valueOf(snapshot.balanceAmount());
            if (payment.getStatus() != providerStatus
                    || payment.getBalanceAmount().compareTo(balanceAmount) != 0
                    || !sameTime(payment.getCanceledAt(), canceledAt)) {
                payment.synchronizeStatus(providerStatus, balanceAmount, canceledAt);
                changed = true;
            }

            if (providerStatus == PaymentStatus.CANCELED) {
                if (order.getStatus() == OrderStatus.PAID) {
                    restoreStock(order);
                    order.cancelPaidOrder(canceledAt);
                    changed = true;
                } else if (order.getStatus() != OrderStatus.CANCELED) {
                    throw new PaymentVerificationException();
                }
            }
        }

        paymentRepository.flush();
        return new PaymentSyncResult(payment, latestCancellation, changed ? "APPLIED" : "NO_CHANGE");
    }

    private void validateSnapshot(
            PurchaseOrder order,
            Payment payment,
            TossPaymentSnapshot snapshot
    ) {
        long expectedAmount;
        try {
            expectedAmount = order.getTotalAmount().longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentVerificationException();
        }
        if (snapshot == null
                || snapshot.paymentKey() == null
                || !order.getOrderNumber().equals(snapshot.orderId())
                || snapshot.totalAmount() != expectedAmount
                || snapshot.balanceAmount() < 0
                || snapshot.balanceAmount() > snapshot.totalAmount()
                || (payment != null && !payment.getPaymentKey().equals(snapshot.paymentKey()))) {
            throw new PaymentVerificationException();
        }

        PaymentStatus status = parseStatus(snapshot.status());
        if (status == PaymentStatus.DONE && snapshot.balanceAmount() != expectedAmount) {
            throw new PaymentVerificationException();
        }
        if (status == PaymentStatus.CANCELED && snapshot.balanceAmount() != 0) {
            throw new PaymentVerificationException();
        }
        if (status == PaymentStatus.CANCELED || status == PaymentStatus.PARTIAL_CANCELED) {
            long canceledAmount = snapshot.cancellations().stream()
                    .filter(cancel -> "DONE".equals(cancel.cancelStatus()))
                    .mapToLong(TossCancellationSnapshot::cancelAmount)
                    .sum();
            if (canceledAmount != expectedAmount - snapshot.balanceAmount()) {
                throw new PaymentVerificationException();
            }
        }
    }

    private PaymentCancellation synchronizeCancellations(
            Payment payment,
            TossPaymentSnapshot snapshot,
            String cancellationIdempotencyKey
    ) {
        PaymentCancellation latestSaved = null;
        List<TossCancellationSnapshot> completed = snapshot.cancellations().stream()
                .filter(cancel -> "DONE".equals(cancel.cancelStatus()))
                .sorted(Comparator.comparing(
                        TossCancellationSnapshot::canceledAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        for (int index = 0; index < completed.size(); index++) {
            TossCancellationSnapshot cancel = completed.get(index);
            validateCancellation(cancel);
            if (paymentCancellationRepository.existsByTransactionKey(cancel.transactionKey())) {
                continue;
            }
            String idempotencyKey = index == completed.size() - 1
                    ? cancellationIdempotencyKey
                    : null;
            latestSaved = paymentCancellationRepository.save(PaymentCancellation.completed(
                    payment,
                    cancel.transactionKey(),
                    idempotencyKey,
                    BigDecimal.valueOf(cancel.cancelAmount()),
                    BigDecimal.valueOf(cancel.refundableAmount()),
                    cancel.cancelReason(),
                    cancel.canceledAt().toLocalDateTime()
            ));
        }
        if (latestSaved != null) {
            paymentCancellationRepository.flush();
        }
        return latestSaved;
    }

    private void validateCancellation(TossCancellationSnapshot cancellation) {
        if (cancellation.cancelAmount() <= 0
                || cancellation.refundableAmount() < 0
                || cancellation.cancelReason() == null
                || cancellation.cancelReason().isBlank()
                || cancellation.cancelReason().length() > 200
                || cancellation.canceledAt() == null
                || cancellation.transactionKey() == null
                || cancellation.transactionKey().isBlank()
                || cancellation.transactionKey().length() > 64) {
            throw new PaymentVerificationException();
        }
    }

    private LocalDateTime requireLatestCancellationTime(
            List<TossCancellationSnapshot> cancellations
    ) {
        return cancellations.stream()
                .filter(cancel -> "DONE".equals(cancel.cancelStatus()))
                .map(TossCancellationSnapshot::canceledAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow(PaymentVerificationException::new)
                .toLocalDateTime();
    }

    private void restoreStock(PurchaseOrder order) {
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = new HashMap<>();
        productRepository.findAllByIdInForUpdate(productIds)
                .forEach(product -> lockedProducts.put(product.getId(), product));

        order.getItems().forEach(item -> {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (product == null) {
                throw new PaymentVerificationException();
            }
            product.restoreStock(item.getQuantity());
        });
    }

    private PaymentStatus parseStatus(String status) {
        try {
            return PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new PaymentVerificationException();
        }
    }

    private boolean isPersistedStatus(PaymentStatus status) {
        return status == PaymentStatus.DONE
                || status == PaymentStatus.CANCELED
                || status == PaymentStatus.PARTIAL_CANCELED;
    }

    private boolean sameTime(LocalDateTime first, LocalDateTime second) {
        return first == null ? second == null : first.equals(second);
    }
}
