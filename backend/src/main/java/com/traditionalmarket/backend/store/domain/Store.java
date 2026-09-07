package com.traditionalmarket.backend.store.domain;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.market.domain.Market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "stores",
        indexes = {
                @Index(name = "idx_stores_market_id", columnList = "market_id"),
                @Index(name = "idx_stores_market_name", columnList = "market_id, name"),
                @Index(name = "idx_stores_market_category", columnList = "market_id, category"),
                @Index(name = "idx_stores_market_status", columnList = "market_id, status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "market_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stores_market")
    )
    private Market market;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StoreCategory category;

    @Column(length = 100)
    private String location;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String openingHours;

    @Column(length = 100)
    private String closedDays;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StoreStatus status;

    @Builder
    private Store(
            Market market,
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
        if (market == null) {
            throw new IllegalArgumentException("소속 시장은 필수입니다.");
        }
        if (category == null) {
            throw new IllegalArgumentException("점포 카테고리는 필수입니다.");
        }

        this.market = market;
        this.name = requireText(name, "점포명");
        this.description = normalizeNullable(description);
        this.category = category;
        this.location = normalizeNullable(location);
        this.phone = normalizeNullable(phone);
        this.openingHours = normalizeNullable(openingHours);
        this.closedDays = normalizeNullable(closedDays);
        this.imageUrl = normalizeNullable(imageUrl);
        this.status = status == null ? StoreStatus.OPEN : status;
    }

    public void update(
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
        if (category == null) {
            throw new IllegalArgumentException("점포 카테고리는 필수입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("점포 운영 상태는 필수입니다.");
        }

        this.name = requireText(name, "점포명");
        this.description = normalizeNullable(description);
        this.category = category;
        this.location = normalizeNullable(location);
        this.phone = normalizeNullable(phone);
        this.openingHours = normalizeNullable(openingHours);
        this.closedDays = normalizeNullable(closedDays);
        this.imageUrl = normalizeNullable(imageUrl);
        this.status = status;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
