package com.traditionalmarket.backend.favorite.domain;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.user.domain.UserAccount;

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
        name = "store_favorites",
        indexes = {
                @Index(name = "idx_store_favorites_user_id", columnList = "user_id"),
                @Index(name = "idx_store_favorites_store_id", columnList = "store_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_favorites_user_store",
                columnNames = {"user_id", "store_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_favorites_user")
    )
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_favorites_store")
    )
    private Store store;

    private StoreFavorite(UserAccount user, Store store) {
        if (user == null) {
            throw new IllegalArgumentException("찜 회원은 필수입니다.");
        }
        if (store == null) {
            throw new IllegalArgumentException("찜 점포는 필수입니다.");
        }

        this.user = user;
        this.store = store;
    }

    public static StoreFavorite create(UserAccount user, Store store) {
        return new StoreFavorite(user, store);
    }
}
