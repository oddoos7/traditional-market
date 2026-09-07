package com.traditionalmarket.backend.favorite.dto.response;

import java.time.LocalDateTime;

import com.traditionalmarket.backend.favorite.domain.StoreFavorite;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;

public record FavoriteStoreResponse(
        Long favoriteId,
        LocalDateTime favoritedAt,
        Long storeId,
        Long marketId,
        String marketName,
        String name,
        String description,
        StoreCategory category,
        String location,
        String phone,
        String openingHours,
        String closedDays,
        String imageUrl,
        StoreStatus status
) {

    public static FavoriteStoreResponse from(StoreFavorite favorite) {
        var store = favorite.getStore();

        return new FavoriteStoreResponse(
                favorite.getId(),
                favorite.getCreatedAt(),
                store.getId(),
                store.getMarket().getId(),
                store.getMarket().getName(),
                store.getName(),
                store.getDescription(),
                store.getCategory(),
                store.getLocation(),
                store.getPhone(),
                store.getOpeningHours(),
                store.getClosedDays(),
                store.getImageUrl(),
                store.getStatus()
        );
    }
}
