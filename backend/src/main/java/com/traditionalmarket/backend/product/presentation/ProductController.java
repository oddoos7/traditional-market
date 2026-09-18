package com.traditionalmarket.backend.product.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.product.application.ProductCommandService;
import com.traditionalmarket.backend.product.application.ProductQueryService;
import com.traditionalmarket.backend.product.application.ProductSort;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.product.dto.request.ProductSaveRequest;
import com.traditionalmarket.backend.product.dto.response.ProductDetailResponse;
import com.traditionalmarket.backend.product.dto.response.ProductPageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores/{storeId}/products")
@Tag(name = "상품", description = "점포 상품 조회 및 관리자 등록·수정 API")
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductCommandService productCommandService;

    @GetMapping
    @Operation(summary = "점포 상품 목록 조회")
    public ProductPageResponse getProducts(
            @PathVariable @Positive Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "LATEST") ProductSort sort
    ) {
        return productQueryService.findProducts(storeId, keyword, status, page, size, sort);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "점포 상품 상세 조회")
    public ProductDetailResponse getProduct(
            @PathVariable @Positive Long storeId,
            @PathVariable @Positive Long productId
    ) {
        return productQueryService.findProduct(storeId, productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "점포 상품 등록")
    public ProductDetailResponse createProduct(
            @PathVariable @Positive Long storeId,
            @Valid @RequestBody ProductSaveRequest request
    ) {
        return productCommandService.createProduct(storeId, request);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "점포 상품 수정")
    public ProductDetailResponse updateProduct(
            @PathVariable @Positive Long storeId,
            @PathVariable @Positive Long productId,
            @Valid @RequestBody ProductSaveRequest request
    ) {
        return productCommandService.updateProduct(storeId, productId, request);
    }
}
