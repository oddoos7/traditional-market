package com.traditionalmarket.backend.store.presentation;

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

import com.traditionalmarket.backend.store.application.StoreCommandService;
import com.traditionalmarket.backend.store.application.StoreQueryService;
import com.traditionalmarket.backend.store.application.StoreSort;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;
import com.traditionalmarket.backend.store.dto.request.StoreSaveRequest;
import com.traditionalmarket.backend.store.dto.response.StoreDetailResponse;
import com.traditionalmarket.backend.store.dto.response.StorePageResponse;

import jakarta.validation.Valid;
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
    private final StoreCommandService storeCommandService;

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

    @GetMapping("/{storeId}")
    public StoreDetailResponse getStore(
            @PathVariable @Positive Long marketId,
            @PathVariable @Positive Long storeId
    ) {
        return storeQueryService.findStore(marketId, storeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreDetailResponse createStore(
            @PathVariable @Positive Long marketId,
            @Valid @RequestBody StoreSaveRequest request
    ) {
        return storeCommandService.createStore(marketId, request);
    }

    @PutMapping("/{storeId}")
    public StoreDetailResponse updateStore(
            @PathVariable @Positive Long marketId,
            @PathVariable @Positive Long storeId,
            @Valid @RequestBody StoreSaveRequest request
    ) {
        return storeCommandService.updateStore(marketId, storeId, request);
    }
}
