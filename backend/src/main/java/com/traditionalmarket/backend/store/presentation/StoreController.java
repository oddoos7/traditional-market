package com.traditionalmarket.backend.store.presentation;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.store.application.StoreQueryService;
import com.traditionalmarket.backend.store.application.StoreSort;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;
import com.traditionalmarket.backend.store.dto.response.StorePageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/markets/{marketId}/stores")
public class StoreController {

    private final StoreQueryService storeQueryService;

    @GetMapping
    public StorePageResponse getStores(
            @PathVariable @Positive Long marketId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StoreCategory category,
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "LATEST") StoreSort sort
    ) {
        return storeQueryService.findStores(
                marketId,
                keyword,
                category,
                status,
                page,
                size,
                sort
        );
    }
}
