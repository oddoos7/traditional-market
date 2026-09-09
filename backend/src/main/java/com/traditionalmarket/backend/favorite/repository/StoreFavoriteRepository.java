package com.traditionalmarket.backend.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.favorite.domain.StoreFavorite;

public interface StoreFavoriteRepository extends JpaRepository<StoreFavorite, Long> {

    boolean existsByUserIdAndStoreId(Long userId, Long storeId);

    long countByUserIdAndStoreId(Long userId, Long storeId);

    long deleteByUserIdAndStoreId(Long userId, Long storeId);

    @EntityGraph(attributePaths = {"store", "store.market"})
    List<StoreFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
