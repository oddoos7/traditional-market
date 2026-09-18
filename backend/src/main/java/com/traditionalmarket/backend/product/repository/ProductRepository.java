package com.traditionalmarket.backend.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :productIds ORDER BY p.id ASC")
    List<Product> findAllByIdInForUpdate(@Param("productIds") Collection<Long> productIds);

    Optional<Product> findByIdAndStoreId(Long productId, Long storeId);

    Optional<Product> findByIdAndStoreIdAndStatusNot(
            Long productId,
            Long storeId,
            ProductStatus hiddenStatus
    );

    @Query(
            value = """
                    SELECT p
                    FROM Product p
                    WHERE p.store.id = :storeId
                    AND p.status <> :hiddenStatus
                    AND (
                        :keyword IS NULL
                        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:status IS NULL OR p.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM Product p
                    WHERE p.store.id = :storeId
                    AND p.status <> :hiddenStatus
                    AND (
                        :keyword IS NULL
                        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:status IS NULL OR p.status = :status)
                    """
    )
    Page<Product> findVisibleProducts(
            @Param("storeId") Long storeId,
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            @Param("hiddenStatus") ProductStatus hiddenStatus,
            Pageable pageable
    );
}
