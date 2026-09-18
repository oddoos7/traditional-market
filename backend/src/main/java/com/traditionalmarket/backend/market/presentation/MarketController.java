package com.traditionalmarket.backend.market.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.market.application.MarketCommandService;
import com.traditionalmarket.backend.market.application.MarketQueryService;
import com.traditionalmarket.backend.market.application.MarketSort;
import com.traditionalmarket.backend.market.domain.MarketStatus;
import com.traditionalmarket.backend.market.dto.request.MarketSaveRequest;
import com.traditionalmarket.backend.market.dto.response.MarketDetailResponse;
import com.traditionalmarket.backend.market.dto.response.MarketPageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/markets")
@Tag(name = "시장", description = "전통시장 조회 및 관리자 등록·수정 API")
public class MarketController {

    private final MarketQueryService marketQueryService;
    private final MarketCommandService marketCommandService;

    @GetMapping
    @Operation(summary = "시장 목록 조회")
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
    @Operation(summary = "시장 상세 조회")
    public MarketDetailResponse getMarket(
            @PathVariable @Positive Long marketId
    ) {
        return marketQueryService.findMarket(marketId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "시장 등록")
    public MarketDetailResponse createMarket(
            @Valid @RequestBody MarketSaveRequest request
    ) {
        return marketCommandService.createMarket(request);
    }

    @PutMapping("/{marketId}")
    @Operation(summary = "시장 수정")
    public MarketDetailResponse updateMarket(
            @PathVariable @Positive Long marketId,
            @Valid @RequestBody MarketSaveRequest request
    ) {
        return marketCommandService.updateMarket(marketId, request);
    }
}
