package com.traditionalmarket.backend.product.dto.request;

import java.math.BigDecimal;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.store.domain.Store;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductSaveRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 5000, message = "상품 설명은 5000자 이하여야 합니다.")
        String description,

        @NotNull(message = "상품 가격은 필수입니다.")
        @DecimalMin(value = "0.01", message = "상품 가격은 0원보다 커야 합니다.")
        @Digits(integer = 10, fraction = 2, message = "상품 가격은 정수 10자리, 소수 2자리 이하여야 합니다.")
        BigDecimal price,

        @NotNull(message = "상품 재고 수량은 필수입니다.")
        @Min(value = 0, message = "상품 재고 수량은 0개 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "상품 재고 수량은 1000000개 이하여야 합니다.")
        Integer stockQuantity,

        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @NotNull(message = "상품 판매 상태는 필수입니다.")
        ProductStatus status
) {

    public Product toEntity(Store store) {
        return Product.builder()
                .store(store)
                .name(name)
                .description(description)
                .price(price)
                .stockQuantity(stockQuantity)
                .imageUrl(imageUrl)
                .status(status)
                .build();
    }

    public void applyTo(Product product) {
        product.update(
                name,
                description,
                price,
                stockQuantity,
                imageUrl,
                status
        );
    }
}
