package com.traditionalmarket.backend.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.order.domain.PurchaseOrder;

import jakarta.persistence.LockModeType;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"store", "store.market"})
    List<PurchaseOrder> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {
            "store",
            "store.market",
            "items"
    })
    Optional<PurchaseOrder> findByIdAndUserId(Long orderId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM PurchaseOrder o
            WHERE o.orderNumber = :orderNumber
              AND o.user.id = :userId
            """)
    Optional<PurchaseOrder> findByOrderNumberAndUserIdForUpdate(
            @Param("orderNumber") String orderNumber,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM PurchaseOrder o
            WHERE o.id = :orderId
              AND o.user.id = :userId
            """)
    Optional<PurchaseOrder> findByIdAndUserIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PurchaseOrder o WHERE o.orderNumber = :orderNumber")
    Optional<PurchaseOrder> findByOrderNumberForUpdate(
            @Param("orderNumber") String orderNumber
    );
}
