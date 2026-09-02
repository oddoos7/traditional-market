package com.traditionalmarket.backend.store.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.market.exception.MarketNotFoundException;
import com.traditionalmarket.backend.market.repository.MarketRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;
import com.traditionalmarket.backend.store.dto.response.StorePageResponse;
import com.traditionalmarket.backend.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService {

    private final MarketRepository marketRepository;
    private final StoreRepository storeRepository;

    public StorePageResponse findStores(
            Long marketId,
            String keyword,
            StoreCategory category,
            StoreStatus status,
            int page,
            int size,
            StoreSort sort
    ) {
        if (!marketRepository.existsById(marketId)) {
            throw new MarketNotFoundException(marketId);
        }

        PageRequest pageRequest = PageRequest.of(page, size, createSort(sort));
        Page<Store> stores = storeRepository.findStores(
                marketId,
                normalizeKeyword(keyword),
                category,
                status,
                pageRequest
        );

        return StorePageResponse.from(stores);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Sort createSort(StoreSort sort) {
        return switch (sort) {
            case NAME -> Sort.by(
                    Sort.Order.asc("name"),
                    Sort.Order.asc("id")
            );
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
