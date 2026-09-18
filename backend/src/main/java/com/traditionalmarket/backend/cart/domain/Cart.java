package com.traditionalmarket.backend.cart.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.user.domain.UserAccount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "carts",
        indexes = @Index(name = "idx_carts_user_id", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_carts_user", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_carts_user")
    )
    private UserAccount user;

    @OrderBy("createdAt ASC, id ASC")
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Cart(UserAccount user) {
        if (user == null) {
            throw new IllegalArgumentException("장바구니 회원은 필수입니다.");
        }
        this.user = user;
    }

    public static Cart create(UserAccount user) {
        return new Cart(user);
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Optional<CartItem> findItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    public Optional<CartItem> findItemById(Long cartItemId) {
        return items.stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst();
    }

    public CartItem addItem(Product product, int quantity) {
        CartItem item = CartItem.create(this, product, quantity);
        items.add(item);
        return item;
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }
}
