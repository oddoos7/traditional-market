package com.traditionalmarket.backend.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("상품을 찾을 수 없습니다. productId=" + productId);
    }

    public ProductNotFoundException(Long storeId, Long productId) {
        super("상품을 찾을 수 없습니다. storeId=" + storeId + ", productId=" + productId);
    }
}
