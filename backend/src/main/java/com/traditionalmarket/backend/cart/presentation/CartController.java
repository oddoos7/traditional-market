package com.traditionalmarket.backend.cart.presentation;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.cart.application.CartService;
import com.traditionalmarket.backend.cart.dto.request.CartItemAddRequest;
import com.traditionalmarket.backend.cart.dto.request.CartItemQuantityRequest;
import com.traditionalmarket.backend.cart.dto.response.CartResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
            Principal principal,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return cartService.addItem(
                principal.getName(),
                request.productId(),
                request.quantity()
        );
    }

    @PutMapping("/items/{cartItemId}")
    public CartResponse updateQuantity(
            Principal principal,
            @PathVariable @Positive Long cartItemId,
            @Valid @RequestBody CartItemQuantityRequest request
    ) {
        return cartService.updateQuantity(
                principal.getName(),
                cartItemId,
                request.quantity()
        );
    }

    @DeleteMapping("/items/{cartItemId}")
    public CartResponse removeItem(
            Principal principal,
            @PathVariable @Positive Long cartItemId
    ) {
        return cartService.removeItem(principal.getName(), cartItemId);
    }

    @DeleteMapping
    public CartResponse clearCart(Principal principal) {
        return cartService.clearCart(principal.getName());
    }
}
