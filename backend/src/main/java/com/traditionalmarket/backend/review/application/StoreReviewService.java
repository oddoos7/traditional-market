package com.traditionalmarket.backend.review.application;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.review.domain.StoreReview;
import com.traditionalmarket.backend.review.dto.request.ReviewSaveRequest;
import com.traditionalmarket.backend.review.dto.response.ReviewPageResponse;
import com.traditionalmarket.backend.review.dto.response.ReviewResponse;
import com.traditionalmarket.backend.review.exception.DuplicateReviewException;
import com.traditionalmarket.backend.review.exception.ReviewNotFoundException;
import com.traditionalmarket.backend.review.repository.StoreReviewRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.store.repository.StoreRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.domain.UserRole;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreReviewService {

    private final StoreReviewRepository storeReviewRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;

    public ReviewPageResponse getReviews(
            Long storeId,
            int page,
            int size,
            String viewerEmail
    ) {
        ensureStoreExists(storeId);
        Long viewerId = viewerEmail == null ? null : getUser(viewerEmail).getId();
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<StoreReview> reviews = storeReviewRepository.findByStoreId(storeId, pageable);
        Double averageRating = storeReviewRepository.findAverageRatingByStoreId(storeId);

        return ReviewPageResponse.from(
                reviews,
                averageRating == null ? 0.0 : averageRating,
                viewerId
        );
    }

    public Optional<ReviewResponse> getMyReview(String email, Long storeId) {
        ensureStoreExists(storeId);
        UserAccount user = getUser(email);

        return storeReviewRepository.findByUserIdAndStoreId(user.getId(), storeId)
                .map(review -> ReviewResponse.from(review, user.getId()));
    }

    @Transactional
    public ReviewResponse createReview(
            String email,
            Long storeId,
            ReviewSaveRequest request
    ) {
        UserAccount user = getUser(email);
        Store store = getStore(storeId);

        if (storeReviewRepository.existsByUserIdAndStoreId(user.getId(), storeId)) {
            throw new DuplicateReviewException(storeId);
        }

        StoreReview review = storeReviewRepository.save(
                StoreReview.create(user, store, request.rating(), request.content())
        );
        return ReviewResponse.from(review, user.getId());
    }

    @Transactional
    public ReviewResponse updateReview(
            String email,
            Long storeId,
            Long reviewId,
            ReviewSaveRequest request
    ) {
        UserAccount user = getUser(email);
        StoreReview review = getReview(storeId, reviewId);
        requireOwner(user, review);

        review.update(request.rating(), request.content());
        return ReviewResponse.from(review, user.getId());
    }

    @Transactional
    public void deleteReview(String email, Long storeId, Long reviewId) {
        UserAccount user = getUser(email);
        StoreReview review = getReview(storeId, reviewId);

        if (!user.getId().equals(review.getUser().getId()) && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        storeReviewRepository.delete(review);
    }

    private StoreReview getReview(Long storeId, Long reviewId) {
        return storeReviewRepository.findByIdAndStoreId(reviewId, storeId)
                .orElseThrow(() -> new ReviewNotFoundException(storeId, reviewId));
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private Store getStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private void ensureStoreExists(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }
    }

    private void requireOwner(UserAccount user, StoreReview review) {
        if (!user.getId().equals(review.getUser().getId())) {
            throw new AccessDeniedException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }
    }
}
