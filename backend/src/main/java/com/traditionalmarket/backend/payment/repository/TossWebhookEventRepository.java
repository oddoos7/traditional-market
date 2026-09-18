package com.traditionalmarket.backend.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.payment.domain.TossWebhookEvent;

public interface TossWebhookEventRepository extends JpaRepository<TossWebhookEvent, Long> {

    Optional<TossWebhookEvent> findByTransmissionId(String transmissionId);
}
