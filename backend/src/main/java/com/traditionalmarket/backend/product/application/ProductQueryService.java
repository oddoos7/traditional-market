package com.traditionalmarket.backend.product.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.product.dto.response.ProductDetailResponse;
import com.traditionalmarket.backend.product.dto.response.ProductPageResponse;
import com.traditionalmarket.backend.product.exception.ProductNotFoundException;
import com.traditionalmarket.backend.product.repository.ProductRepository;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public ProductDetailResponse findProduct(Long storeId, Long productId) {
        validateStoreExists(storeId);

        Product product = productRepository.findByIdAndStoreIdAndStatusNot(
                        productId,
                        storeId,
                        ProductStatus.HIDDEN
                )
                .orElseThrow(() -> new ProductNotFoundException(storeId, productId));

        return ProductDetailResponse.from(product);
    }

    public ProductPageResponse findProducts(
            Long storeId,
            String keyword,
            ProductStatus status,
            int page,
            int size,
            ProductSort sort
    ) {
        validateStoreExists(storeId);

        PageRequest pageRequest = PageRequest.of(page, size, createSort(sort));
        Page<Product> products = productRepository.findVisibleProducts(
                storeId,
                normalizeKeyword(keyword),
                status,
                ProductStatus.HIDDEN,
                pageRequest
        );

        return ProductPageResponse.from(products);
    }

    private void validateStoreExists(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Sort createSort(ProductSort sort) {
        return switch (sort) {
            case NAME -> Sort.by(
                    Sort.Order.asc("name"),
                    Sort.Order.asc("id")
            );
            case PRICE_LOW -> Sort.by(
                    Sort.Order.asc("price"),
                    Sort.Order.asc("id")
            );
            case PRICE_HIGH -> Sort.by(
                    Sort.Order.desc("price"),
                    Sort.Order.desc("id")
            );
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
