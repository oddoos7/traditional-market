package com.traditionalmarket.backend.store.dto.response;

import java.time.LocalDateTime;

import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;

public record StoreSummaryResponse(
        Long id,
        Long marketId,
        String name,
        String description,
        StoreCategory category,
        String location,
        String phone,
        String openingHours,
        String closedDays,
        String imageUrl,
        StoreStatus status,
        LocalDateTime createdAt
) {

    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(
                store.getId(),
                store.getMarket().getId(),
                store.getName(),
                store.getDescription(),
                store.getCategory(),
                store.getLocation(),
                store.getPhone(),
                store.getOpeningHours(),
                store.getClosedDays(),
                store.getImageUrl(),
                store.getStatus(),
                store.getCreatedAt()
        );
    }
}
