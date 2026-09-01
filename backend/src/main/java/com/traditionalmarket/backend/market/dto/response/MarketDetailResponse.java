package com.traditionalmarket.backend.market.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;

public record MarketDetailResponse(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MarketDetailResponse from(Market market) {
        return new MarketDetailResponse(
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
                market.getCreatedAt(),
                market.getUpdatedAt()
        );
    }
}
