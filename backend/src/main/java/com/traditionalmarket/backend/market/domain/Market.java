package com.traditionalmarket.backend.market.domain;

import java.math.BigDecimal;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "markets",
        indexes = {
                @Index(name = "idx_markets_name", columnList = "name"),
                @Index(name = "idx_markets_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 255)
    private String detailAddress;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String openingHours;

    @Column(length = 100)
    private String closedDays;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MarketStatus status;

    @Builder
    private Market(
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
            MarketStatus status
    ) {
        this.name = requireText(name, "시장명");
        this.description = description;
        this.address = requireText(address, "주소");
        this.detailAddress = detailAddress;
        this.phone = phone;
        this.openingHours = openingHours;
        this.closedDays = closedDays;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.status = status == null ? MarketStatus.OPEN : status;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
