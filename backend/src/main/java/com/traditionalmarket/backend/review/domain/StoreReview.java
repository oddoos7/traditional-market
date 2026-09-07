package com.traditionalmarket.backend.review.domain;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.user.domain.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "store_reviews",
        indexes = {
                @Index(name = "idx_store_reviews_store_id", columnList = "store_id"),
                @Index(name = "idx_store_reviews_user_id", columnList = "user_id"),
                @Index(name = "idx_store_reviews_store_created", columnList = "store_id, created_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_reviews_user_store",
                columnNames = {"user_id", "store_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_reviews_user")
    )
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_reviews_store")
    )
    private Store store;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String content;

    private StoreReview(UserAccount user, Store store, int rating, String content) {
        if (user == null) {
            throw new IllegalArgumentException("리뷰 작성자는 필수입니다.");
        }
        if (store == null) {
            throw new IllegalArgumentException("리뷰 점포는 필수입니다.");
        }

        this.user = user;
        this.store = store;
        update(rating, content);
    }

    public static StoreReview create(
            UserAccount user,
            Store store,
            int rating,
            String content
    ) {
        return new StoreReview(user, store, rating, content);
    }

    public void update(int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1점부터 5점까지 입력할 수 있습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("리뷰 내용은 필수입니다.");
        }

        this.rating = rating;
        this.content = content.trim();
    }
}
