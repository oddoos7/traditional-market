package com.traditionalmarket.backend.review.presentation;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.review.application.StoreReviewService;
import com.traditionalmarket.backend.review.dto.request.ReviewSaveRequest;
import com.traditionalmarket.backend.review.dto.response.ReviewPageResponse;
import com.traditionalmarket.backend.review.dto.response.ReviewResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/stores/{storeId}/reviews")
@RequiredArgsConstructor
@Tag(name = "리뷰", description = "점포 리뷰 조회, 등록, 수정 및 삭제 API")
public class StoreReviewController {

    private final StoreReviewService storeReviewService;

    @GetMapping
    @Operation(summary = "점포 리뷰 목록 조회")
    public ReviewPageResponse getReviews(
            @PathVariable @Positive Long storeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int size,
            Principal principal
    ) {
        return storeReviewService.getReviews(
                storeId,
                page,
                size,
                principal == null ? null : principal.getName()
        );
    }

    @GetMapping("/me")
    @Operation(summary = "내 점포 리뷰 조회")
    public ResponseEntity<ReviewResponse> getMyReview(
            @PathVariable @Positive Long storeId,
            Principal principal
    ) {
        return storeReviewService.getMyReview(principal.getName(), storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @Operation(summary = "점포 리뷰 등록")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable @Positive Long storeId,
            Principal principal,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        ReviewResponse response = storeReviewService.createReview(
                principal.getName(),
                storeId,
                request
        );

        return ResponseEntity.created(
                URI.create("/api/stores/" + storeId + "/reviews/" + response.id())
        ).body(response);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "내 점포 리뷰 수정")
    public ReviewResponse updateReview(
            @PathVariable @Positive Long storeId,
            @PathVariable @Positive Long reviewId,
            Principal principal,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        return storeReviewService.updateReview(
                principal.getName(),
                storeId,
                reviewId,
                request
        );
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "내 점포 리뷰 삭제")
    public ResponseEntity<Void> deleteReview(
            @PathVariable @Positive Long storeId,
            @PathVariable @Positive Long reviewId,
            Principal principal
    ) {
        storeReviewService.deleteReview(principal.getName(), storeId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
