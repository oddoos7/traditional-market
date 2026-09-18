package com.traditionalmarket.backend.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderId(Long orderId);
}
