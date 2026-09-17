package com.traditionalmarket.backend.product.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.dto.request.ProductSaveRequest;
import com.traditionalmarket.backend.product.dto.response.ProductDetailResponse;
import com.traditionalmarket.backend.product.exception.ProductNotFoundException;
import com.traditionalmarket.backend.product.repository.ProductRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public ProductDetailResponse createProduct(Long storeId, ProductSaveRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));

        Product product = productRepository.saveAndFlush(request.toEntity(store));
        return ProductDetailResponse.from(product);
    }

    public ProductDetailResponse updateProduct(
            Long storeId,
            Long productId,
            ProductSaveRequest request
    ) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }

        Product product = productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ProductNotFoundException(storeId, productId));

        request.applyTo(product);
        productRepository.flush();

        return ProductDetailResponse.from(product);
    }
}
