package com.traditionalmarket.backend.order.domain;

import java.math.BigDecimal;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;
import com.traditionalmarket.backend.product.domain.Product;

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
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_id", columnList = "product_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_items_order_product",
                columnNames = {"order_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_items_order")
    )
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_items_product")
    )
    private Product product;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 500)
    private String productImageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    private OrderItem(PurchaseOrder order, Product product, int quantity) {
        if (order == null || product == null) {
            throw new IllegalArgumentException("주문과 상품은 필수입니다.");
        }
        if (quantity < 1 || quantity > 99) {
            throw new IllegalArgumentException("주문 상품 수량은 1개 이상 99개 이하여야 합니다.");
        }
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.productImageUrl = product.getImageUrl();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
        this.lineAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    static OrderItem create(PurchaseOrder order, Product product, int quantity) {
        return new OrderItem(order, product, quantity);
    }
}
