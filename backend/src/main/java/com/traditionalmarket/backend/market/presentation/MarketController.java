package com.traditionalmarket.backend.market.presentation;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.market.application.MarketQueryService;
import com.traditionalmarket.backend.market.application.MarketSort;
import com.traditionalmarket.backend.market.domain.MarketStatus;
import com.traditionalmarket.backend.market.dto.response.MarketDetailResponse;
import com.traditionalmarket.backend.market.dto.response.MarketPageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/markets")
public class MarketController {

    private final MarketQueryService marketQueryService;

    @GetMapping
    public MarketPageResponse getMarkets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "LATEST") MarketSort sort
    ) {
        return marketQueryService.findMarkets(keyword, status, page, size, sort);
    }

    @GetMapping("/{marketId}")
    public MarketDetailResponse getMarket(
            @PathVariable @Positive Long marketId
    ) {
        return marketQueryService.findMarket(marketId);
    }
}
