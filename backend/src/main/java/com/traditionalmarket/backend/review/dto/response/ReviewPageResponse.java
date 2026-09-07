package com.traditionalmarket.backend.review.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.traditionalmarket.backend.review.domain.StoreReview;

public record ReviewPageResponse(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        double averageRating
) {

    public static ReviewPageResponse from(
            Page<StoreReview> reviewPage,
            double averageRating,
            Long viewerId
    ) {
        return new ReviewPageResponse(
                reviewPage.getContent().stream()
                        .map(review -> ReviewResponse.from(review, viewerId))
                        .toList(),
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.isFirst(),
                reviewPage.isLast(),
                averageRating
        );
    }
}
