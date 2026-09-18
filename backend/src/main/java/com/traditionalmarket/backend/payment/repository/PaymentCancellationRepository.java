package com.traditionalmarket.backend.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.payment.domain.PaymentCancellation;

public interface PaymentCancellationRepository
        extends JpaRepository<PaymentCancellation, Long> {

    boolean existsByTransactionKey(String transactionKey);

    List<PaymentCancellation> findAllByPaymentIdOrderByCanceledAtAscIdAsc(Long paymentId);

    Optional<PaymentCancellation> findFirstByPaymentIdOrderByCanceledAtDescIdDesc(Long paymentId);
}
