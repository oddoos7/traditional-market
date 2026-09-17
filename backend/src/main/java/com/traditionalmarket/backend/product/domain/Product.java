package com.traditionalmarket.backend.product.domain;

import java.math.BigDecimal;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.store.domain.Store;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_store_id", columnList = "store_id"),
                @Index(name = "idx_products_store_name", columnList = "store_id, name"),
                @Index(name = "idx_products_store_status", columnList = "store_id, status"),
                @Index(name = "idx_products_store_price", columnList = "store_id, price")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
    private static final int MAX_STOCK_QUANTITY = 1_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_store")
    )
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Builder
    private Product(
            Store store,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        this.store = requireStore(store);
        this.name = requireText(name, "상품명");
        this.description = normalizeNullable(description);
        this.price = requirePrice(price);
        this.stockQuantity = requireStockQuantity(stockQuantity);
        this.imageUrl = normalizeNullable(imageUrl);
        this.status = status == null ? ProductStatus.ON_SALE : status;
    }

    public void update(
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        if (status == null) {
            throw new IllegalArgumentException("상품 판매 상태는 필수입니다.");
        }

        this.name = requireText(name, "상품명");
        this.description = normalizeNullable(description);
        this.price = requirePrice(price);
        this.stockQuantity = requireStockQuantity(stockQuantity);
        this.imageUrl = normalizeNullable(imageUrl);
        this.status = status;
    }

    private static Store requireStore(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("소속 점포는 필수입니다.");
        }
        return store;
    }

    private static BigDecimal requirePrice(BigDecimal price) {
        if (price == null
                || price.signum() <= 0
                || price.scale() > 2
                || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("상품 가격이 올바르지 않습니다.");
        }
        return price.setScale(2);
    }

    private static int requireStockQuantity(int stockQuantity) {
        if (stockQuantity < 0 || stockQuantity > MAX_STOCK_QUANTITY) {
            throw new IllegalArgumentException("상품 재고 수량이 올바르지 않습니다.");
        }
        return stockQuantity;
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
