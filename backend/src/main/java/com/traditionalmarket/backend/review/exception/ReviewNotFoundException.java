package com.traditionalmarket.backend.review.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long storeId, Long reviewId) {
        super("리뷰를 찾을 수 없습니다. storeId=" + storeId + ", reviewId=" + reviewId);
    }
}
