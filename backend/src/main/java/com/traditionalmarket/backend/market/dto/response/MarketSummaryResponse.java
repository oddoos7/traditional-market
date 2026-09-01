package com.traditionalmarket.backend.market.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;

public record MarketSummaryResponse(
        Long id,
        String name,
        String description,
        String address,
        String detailAddress,
        String phone,
        String openingHours,
        String closedDays,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        MarketStatus status,
        LocalDateTime createdAt
) {

    public static MarketSummaryResponse from(Market market) {
        return new MarketSummaryResponse(
                market.getId(),
                market.getName(),
                market.getDescription(),
                market.getAddress(),
                market.getDetailAddress(),
                market.getPhone(),
                market.getOpeningHours(),
                market.getClosedDays(),
                market.getLatitude(),
                market.getLongitude(),
                market.getImageUrl(),
                market.getStatus(),
                market.getCreatedAt()
        );
    }
}
