package com.traditionalmarket.backend.store.dto.request;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreSaveRequest(
        @NotBlank(message = "점포명은 필수입니다.")
        @Size(max = 100, message = "점포명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 5000, message = "점포 소개는 5000자 이하여야 합니다.")
        String description,

        @NotNull(message = "점포 카테고리는 필수입니다.")
        StoreCategory category,

        @Size(max = 100, message = "점포 위치는 100자 이하여야 합니다.")
        String location,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,

        @Size(max = 100, message = "운영시간은 100자 이하여야 합니다.")
        String openingHours,

        @Size(max = 100, message = "휴무일은 100자 이하여야 합니다.")
        String closedDays,

        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @NotNull(message = "점포 운영 상태는 필수입니다.")
        StoreStatus status
) {

    public Store toEntity(Market market) {
        return Store.builder()
                .market(market)
                .name(name)
                .description(description)
                .category(category)
                .location(location)
                .phone(phone)
                .openingHours(openingHours)
                .closedDays(closedDays)
                .imageUrl(imageUrl)
                .status(status)
                .build();
    }

    public void applyTo(Store store) {
        store.update(
                name,
                description,
                category,
                location,
                phone,
                openingHours,
                closedDays,
                imageUrl,
                status
        );
    }
}
