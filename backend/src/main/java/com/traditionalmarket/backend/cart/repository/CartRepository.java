package com.traditionalmarket.backend.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.cart.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.product.store",
            "items.product.store.market"
    })
    Optional<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findForOrderByUserId(@Param("userId") Long userId);
}
