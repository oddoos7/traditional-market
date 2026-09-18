package com.traditionalmarket.backend.order.exception;

public class OrderStockUnavailableException extends RuntimeException {

    public OrderStockUnavailableException(Long productId, int requested, int available) {
        super("주문 가능한 재고가 부족합니다. productId=%d, requested=%d, available=%d"
                .formatted(productId, requested, available));
    }
}
