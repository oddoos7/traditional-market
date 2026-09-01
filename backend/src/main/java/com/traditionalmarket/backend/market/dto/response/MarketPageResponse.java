package com.traditionalmarket.backend.market.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.traditionalmarket.backend.market.domain.Market;

public record MarketPageResponse(
        List<MarketSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static MarketPageResponse from(Page<Market> markets) {
        List<MarketSummaryResponse> content = markets.getContent().stream()
                .map(MarketSummaryResponse::from)
                .toList();

        return new MarketPageResponse(
                content,
                markets.getNumber(),
                markets.getSize(),
                markets.getTotalElements(),
                markets.getTotalPages(),
                markets.isFirst(),
                markets.isLast()
        );
    }
}
