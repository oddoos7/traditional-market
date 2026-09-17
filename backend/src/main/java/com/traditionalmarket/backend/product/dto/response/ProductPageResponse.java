package com.traditionalmarket.backend.product.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.traditionalmarket.backend.product.domain.Product;

public record ProductPageResponse(
        List<ProductSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static ProductPageResponse from(Page<Product> products) {
        List<ProductSummaryResponse> content = products.getContent().stream()
                .map(ProductSummaryResponse::from)
                .toList();

        return new ProductPageResponse(
                content,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast()
        );
    }
}
