package com.traditionalmarket.backend.market.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.dto.request.MarketSaveRequest;
import com.traditionalmarket.backend.market.dto.response.MarketDetailResponse;
import com.traditionalmarket.backend.market.exception.MarketNotFoundException;
import com.traditionalmarket.backend.market.repository.MarketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketCommandService {

    private final MarketRepository marketRepository;

    public MarketDetailResponse createMarket(MarketSaveRequest request) {
        Market market = marketRepository.saveAndFlush(request.toEntity());
        return MarketDetailResponse.from(market);
    }

    public MarketDetailResponse updateMarket(Long marketId, MarketSaveRequest request) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new MarketNotFoundException(marketId));

        request.applyTo(market);
        marketRepository.flush();

        return MarketDetailResponse.from(market);
    }
}
