package com.traditionalmarket.backend.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.traditionalmarket.backend.market.domain.Market;

public interface MarketRepository extends JpaRepository<Market, Long> {
}
