package com.traditionalmarket.backend.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.review.domain.StoreReview;

public interface StoreReviewRepository extends JpaRepository<StoreReview, Long> {

    @EntityGraph(attributePaths = {"user", "store"})
    Page<StoreReview> findByStoreId(Long storeId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "store"})
    Optional<StoreReview> findByIdAndStoreId(Long reviewId, Long storeId);

    @EntityGraph(attributePaths = {"user", "store"})
    Optional<StoreReview> findByUserIdAndStoreId(Long userId, Long storeId);

    boolean existsByUserIdAndStoreId(Long userId, Long storeId);

    long countByUserIdAndStoreId(Long userId, Long storeId);

    @Query("SELECT AVG(r.rating) FROM StoreReview r WHERE r.store.id = :storeId")
    Double findAverageRatingByStoreId(@Param("storeId") Long storeId);
}
