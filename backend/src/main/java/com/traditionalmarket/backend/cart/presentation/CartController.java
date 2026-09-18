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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Tag(name = "장바구니", description = "로그인 회원의 장바구니 API")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "장바구니 조회")
    public CartResponse getCart(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "장바구니 상품 추가")
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
    @Operation(summary = "장바구니 상품 수량 변경")
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
    @Operation(summary = "장바구니 상품 삭제")
    public CartResponse removeItem(
            Principal principal,
            @PathVariable @Positive Long cartItemId
    ) {
        return cartService.removeItem(principal.getName(), cartItemId);
    }

    @DeleteMapping
    @Operation(summary = "장바구니 전체 비우기")
    public CartResponse clearCart(Principal principal) {
        return cartService.clearCart(principal.getName());
    }
}
