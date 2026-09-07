package com.traditionalmarket.backend.review.dto.response;

import java.time.LocalDateTime;

import com.traditionalmarket.backend.review.domain.StoreReview;

public record ReviewResponse(
        Long id,
        Long storeId,
        String reviewerName,
        int rating,
        String content,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReviewResponse from(StoreReview review, Long viewerId) {
        return new ReviewResponse(
                review.getId(),
                review.getStore().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getContent(),
                viewerId != null && viewerId.equals(review.getUser().getId()),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
