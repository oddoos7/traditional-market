package com.traditionalmarket.backend.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.order.domain.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"store", "store.market"})
    List<PurchaseOrder> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {
            "store",
            "store.market",
            "items"
    })
    Optional<PurchaseOrder> findByIdAndUserId(Long orderId, Long userId);
}
