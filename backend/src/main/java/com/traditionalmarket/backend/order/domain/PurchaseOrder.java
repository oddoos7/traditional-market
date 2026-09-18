package com.traditionalmarket.backend.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.user.domain.UserAccount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_orders_store_created", columnList = "store_id, created_at"),
                @Index(name = "idx_orders_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_order_number",
                columnNames = "order_number"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user")
    )
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_store")
    )
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private int totalItemCount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    private LocalDateTime canceledAt;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private PurchaseOrder(String orderNumber, UserAccount user, Store store) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("주문번호는 필수입니다.");
        }
        if (user == null || store == null) {
            throw new IllegalArgumentException("주문 회원과 점포는 필수입니다.");
        }
        this.orderNumber = orderNumber;
        this.user = user;
        this.store = store;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
    }

    public static PurchaseOrder create(String orderNumber, UserAccount user, Store store) {
        return new PurchaseOrder(orderNumber, user, store);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(
            com.traditionalmarket.backend.product.domain.Product product,
            int quantity
    ) {
        OrderItem item = OrderItem.create(this, product, quantity);
        items.add(item);
        totalItemCount += quantity;
        totalAmount = totalAmount.add(item.getLineAmount());
    }

    public void cancel(LocalDateTime canceledAt) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("결제 대기 중인 주문만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = canceledAt;
    }
}
