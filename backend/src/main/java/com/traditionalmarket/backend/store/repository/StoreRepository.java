package com.traditionalmarket.backend.store.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByIdAndMarketId(Long storeId, Long marketId);

    @Query(
            value = """
                    SELECT s
                    FROM Store s
                    WHERE s.market.id = :marketId
                    AND (
                        :keyword IS NULL
                        OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:category IS NULL OR s.category = :category)
                    AND (:status IS NULL OR s.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(s)
                    FROM Store s
                    WHERE s.market.id = :marketId
                    AND (
                        :keyword IS NULL
                        OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:category IS NULL OR s.category = :category)
                    AND (:status IS NULL OR s.status = :status)
                    """
    )
    Page<Store> findStores(
            @Param("marketId") Long marketId,
            @Param("keyword") String keyword,
            @Param("category") StoreCategory category,
            @Param("status") StoreStatus status,
            Pageable pageable
    );
}
