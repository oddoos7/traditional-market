package com.traditionalmarket.backend.market.dto.request;

import java.math.BigDecimal;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketSaveRequest(
        @NotBlank(message = "시장명은 필수입니다.")
        @Size(max = 100, message = "시장명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 5000, message = "시장 소개는 5000자 이하여야 합니다.")
        String description,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String detailAddress,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,

        @Size(max = 100, message = "운영시간은 100자 이하여야 합니다.")
        String openingHours,

        @Size(max = 100, message = "휴무일은 100자 이하여야 합니다.")
        String closedDays,

        @DecimalMin(value = "-90.0000000", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0000000", message = "위도는 90 이하여야 합니다.")
        @Digits(integer = 2, fraction = 7, message = "위도는 소수점 아래 7자리까지 입력할 수 있습니다.")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0000000", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0000000", message = "경도는 180 이하여야 합니다.")
        @Digits(integer = 3, fraction = 7, message = "경도는 소수점 아래 7자리까지 입력할 수 있습니다.")
        BigDecimal longitude,

        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @NotNull(message = "운영 상태는 필수입니다.")
        MarketStatus status
) {

    public Market toEntity() {
        return Market.builder()
                .name(name)
                .description(description)
                .address(address)
                .detailAddress(detailAddress)
                .phone(phone)
                .openingHours(openingHours)
                .closedDays(closedDays)
                .latitude(latitude)
                .longitude(longitude)
                .imageUrl(imageUrl)
                .status(status)
                .build();
    }

    public void applyTo(Market market) {
        market.update(
                name,
                description,
                address,
                detailAddress,
                phone,
                openingHours,
                closedDays,
                latitude,
                longitude,
                imageUrl,
                status
        );
    }
}
