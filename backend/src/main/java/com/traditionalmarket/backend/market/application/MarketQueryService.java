package com.traditionalmarket.backend.market.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;
import com.traditionalmarket.backend.market.dto.response.MarketDetailResponse;
import com.traditionalmarket.backend.market.dto.response.MarketPageResponse;
import com.traditionalmarket.backend.market.exception.MarketNotFoundException;
import com.traditionalmarket.backend.market.repository.MarketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketQueryService {

    private final MarketRepository marketRepository;

    public MarketPageResponse findMarkets(
            String keyword,
            MarketStatus status,
            int page,
            int size,
            MarketSort sort
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        PageRequest pageRequest = PageRequest.of(page, size, createSort(sort));
        Page<Market> markets = marketRepository.findMarkets(normalizedKeyword, status, pageRequest);

        return MarketPageResponse.from(markets);
    }

    public MarketDetailResponse findMarket(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new MarketNotFoundException(marketId));

        return MarketDetailResponse.from(market);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Sort createSort(MarketSort sort) {
        return switch (sort) {
            case NAME -> Sort.by(
                    Sort.Order.asc("name"),
                    Sort.Order.asc("id")
            );
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
