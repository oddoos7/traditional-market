package com.traditionalmarket.backend.cart.exception;

public class CartStoreMismatchException extends RuntimeException {

    public CartStoreMismatchException(Long currentStoreId, Long requestedStoreId) {
        super(
                "장바구니에는 한 점포의 상품만 담을 수 있습니다. currentStoreId="
                        + currentStoreId
                        + ", requestedStoreId="
                        + requestedStoreId
        );
    }
}
