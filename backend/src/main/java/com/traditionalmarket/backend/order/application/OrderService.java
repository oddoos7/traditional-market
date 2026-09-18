package com.traditionalmarket.backend.order.application;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.cart.domain.Cart;
import com.traditionalmarket.backend.cart.repository.CartRepository;
import com.traditionalmarket.backend.order.domain.OrderStatus;
import com.traditionalmarket.backend.order.domain.PurchaseOrder;
import com.traditionalmarket.backend.order.dto.response.OrderDetailResponse;
import com.traditionalmarket.backend.order.dto.response.OrderSummaryResponse;
import com.traditionalmarket.backend.order.exception.EmptyCartException;
import com.traditionalmarket.backend.order.exception.OrderNotCancelableException;
import com.traditionalmarket.backend.order.exception.OrderNotFoundException;
import com.traditionalmarket.backend.order.exception.OrderStockUnavailableException;
import com.traditionalmarket.backend.order.repository.PurchaseOrderRepository;
import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.product.repository.ProductRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public OrderDetailResponse createOrder(String email) {
        UserAccount user = getUserForUpdate(email);
        Cart cart = cartRepository.findForOrderByUserId(user.getId())
                .filter(found -> !found.getItems().isEmpty())
                .orElseThrow(EmptyCartException::new);

        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = new HashMap<>();
        productRepository.findAllByIdInForUpdate(productIds)
                .forEach(product -> lockedProducts.put(product.getId(), product));

        Product firstProduct = lockedProducts.get(productIds.get(0));
        if (firstProduct == null) {
            throw new OrderStockUnavailableException(productIds.get(0), 1, 0);
        }

        PurchaseOrder order = PurchaseOrder.create(
                createOrderNumber(),
                user,
                firstProduct.getStore()
        );

        cart.getItems().forEach(cartItem -> {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            validateStock(product, cartItem.getProduct().getId(), cartItem.getQuantity());
            order.addItem(product, cartItem.getQuantity());
            product.decreaseStock(cartItem.getQuantity());
        });

        PurchaseOrder savedOrder = purchaseOrderRepository.saveAndFlush(order);
        cart.clear();
        cartRepository.flush();
        return OrderDetailResponse.from(savedOrder);
    }

    public List<OrderSummaryResponse> getOrders(String email) {
        UserAccount user = getUser(email);
        return purchaseOrderRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    public OrderDetailResponse getOrder(String email, Long orderId) {
        UserAccount user = getUser(email);
        return OrderDetailResponse.from(getOrder(orderId, user.getId()));
    }

    @Transactional
    public OrderDetailResponse cancelOrder(String email, Long orderId) {
        UserAccount user = getUserForUpdate(email);
        PurchaseOrder order = getOrder(orderId, user.getId());
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotCancelableException(orderId);
        }

        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = new HashMap<>();
        productRepository.findAllByIdInForUpdate(productIds)
                .forEach(product -> lockedProducts.put(product.getId(), product));

        order.getItems().forEach(item -> {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (product == null) {
                throw new OrderNotCancelableException(orderId);
            }
            product.restoreStock(item.getQuantity());
        });
        order.cancel(LocalDateTime.now());
        purchaseOrderRepository.flush();
        return OrderDetailResponse.from(order);
    }

    private PurchaseOrder getOrder(Long orderId, Long userId) {
        return purchaseOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private UserAccount getUserForUpdate(String email) {
        return userAccountRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private void validateStock(Product product, Long productId, int requestedQuantity) {
        int available = product == null ? 0 : product.getStockQuantity();
        if (product == null
                || product.getStatus() != ProductStatus.ON_SALE
                || available < requestedQuantity) {
            throw new OrderStockUnavailableException(productId, requestedQuantity, available);
        }
    }

    private String createOrderNumber() {
        return "ORD-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }
}
