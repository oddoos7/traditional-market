package com.traditionalmarket.backend.review.exception;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(Long storeId) {
        super("한 점포에는 리뷰를 한 개만 작성할 수 있습니다. storeId=" + storeId);
    }
}
