package com.traditionalmarket.backend.store.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.traditionalmarket.backend.store.domain.Store;

public record StorePageResponse(
        List<StoreSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static StorePageResponse from(Page<Store> stores) {
        List<StoreSummaryResponse> content = stores.getContent().stream()
                .map(StoreSummaryResponse::from)
                .toList();

        return new StorePageResponse(
                content,
                stores.getNumber(),
                stores.getSize(),
                stores.getTotalElements(),
                stores.getTotalPages(),
                stores.isFirst(),
                stores.isLast()
        );
    }
}
