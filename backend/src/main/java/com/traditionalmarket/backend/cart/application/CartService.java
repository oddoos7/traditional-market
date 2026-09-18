package com.traditionalmarket.backend.cart.application;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.cart.domain.Cart;
import com.traditionalmarket.backend.cart.domain.CartItem;
import com.traditionalmarket.backend.cart.dto.response.CartResponse;
import com.traditionalmarket.backend.cart.exception.CartItemNotFoundException;
import com.traditionalmarket.backend.cart.exception.CartProductUnavailableException;
import com.traditionalmarket.backend.cart.exception.CartQuantityExceededException;
import com.traditionalmarket.backend.cart.exception.CartStoreMismatchException;
import com.traditionalmarket.backend.cart.repository.CartRepository;
import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.product.exception.ProductNotFoundException;
import com.traditionalmarket.backend.product.repository.ProductRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;

    public CartResponse getCart(String email) {
        UserAccount user = getUser(email);
        return cartRepository.findByUserId(user.getId())
                .map(CartResponse::from)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(String email, Long productId, int quantity) {
        UserAccount user = getUserForUpdate(email);
        Product product = getProduct(productId);
        validateProductAvailable(product);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> Cart.create(user));
        validateSameStore(cart, product);

        var existingItem = cart.findItemByProductId(productId);
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int combinedQuantity = item.getQuantity() + quantity;
            validateQuantity(product, combinedQuantity);
            item.updateQuantity(combinedQuantity);
        } else {
            validateQuantity(product, quantity);
            cart.addItem(product, quantity);
        }

        cartRepository.saveAndFlush(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateQuantity(String email, Long cartItemId, int quantity) {
        UserAccount user = getUserForUpdate(email);
        Cart cart = getCart(user.getId());
        CartItem item = getItem(cart, cartItemId);

        validateProductAvailable(item.getProduct());
        validateQuantity(item.getProduct(), quantity);
        item.updateQuantity(quantity);
        cartRepository.flush();

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, Long cartItemId) {
        UserAccount user = getUserForUpdate(email);
        Cart cart = getCart(user.getId());
        CartItem item = getItem(cart, cartItemId);

        cart.removeItem(item);
        cartRepository.flush();
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clearCart(String email) {
        UserAccount user = getUserForUpdate(email);
        return cartRepository.findByUserId(user.getId())
                .map(cart -> {
                    cart.clear();
                    cartRepository.flush();
                    return CartResponse.from(cart);
                })
                .orElseGet(CartResponse::empty);
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private UserAccount getUserForUpdate(String email) {
        return userAccountRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private Cart getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartItemNotFoundException(null));
    }

    private CartItem getItem(Cart cart, Long cartItemId) {
        return cart.findItemById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validateProductAvailable(Product product) {
        if (product.getStatus() != ProductStatus.ON_SALE || product.getStockQuantity() < 1) {
            throw new CartProductUnavailableException(product.getId());
        }
    }

    private void validateQuantity(Product product, int quantity) {
        int availableQuantity = Math.min(product.getStockQuantity(), CartItem.MAX_QUANTITY);
        if (quantity > availableQuantity) {
            throw new CartQuantityExceededException(
                    product.getId(),
                    quantity,
                    availableQuantity
            );
        }
    }

    private void validateSameStore(Cart cart, Product product) {
        if (cart.getItems().isEmpty()) {
            return;
        }

        Long currentStoreId = cart.getItems().get(0).getProduct().getStore().getId();
        Long requestedStoreId = product.getStore().getId();
        if (!currentStoreId.equals(requestedStoreId)) {
            throw new CartStoreMismatchException(currentStoreId, requestedStoreId);
        }
    }
}
