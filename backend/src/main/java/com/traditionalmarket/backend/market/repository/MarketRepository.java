package com.traditionalmarket.backend.market.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;

public interface MarketRepository extends JpaRepository<Market, Long> {

    @Query(
            value = """
                    SELECT m
                    FROM Market m
                    WHERE (
                        :keyword IS NULL
                        OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:status IS NULL OR m.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(m)
                    FROM Market m
                    WHERE (
                        :keyword IS NULL
                        OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    AND (:status IS NULL OR m.status = :status)
                    """
    )
    Page<Market> findMarkets(
            @Param("keyword") String keyword,
            @Param("status") MarketStatus status,
            Pageable pageable
    );
}
