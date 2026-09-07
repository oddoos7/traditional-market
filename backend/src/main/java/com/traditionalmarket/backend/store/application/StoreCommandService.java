package com.traditionalmarket.backend.store.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.exception.MarketNotFoundException;
import com.traditionalmarket.backend.market.repository.MarketRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.dto.request.StoreSaveRequest;
import com.traditionalmarket.backend.store.dto.response.StoreDetailResponse;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreCommandService {

    private final MarketRepository marketRepository;
    private final StoreRepository storeRepository;

    public StoreDetailResponse createStore(Long marketId, StoreSaveRequest request) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new MarketNotFoundException(marketId));

        Store store = storeRepository.saveAndFlush(request.toEntity(market));
        return StoreDetailResponse.from(store);
    }

    public StoreDetailResponse updateStore(
            Long marketId,
            Long storeId,
            StoreSaveRequest request
    ) {
        if (!marketRepository.existsById(marketId)) {
            throw new MarketNotFoundException(marketId);
        }

        Store store = storeRepository.findByIdAndMarketId(storeId, marketId)
                .orElseThrow(() -> new StoreNotFoundException(marketId, storeId));

        request.applyTo(store);
        storeRepository.flush();

        return StoreDetailResponse.from(store);
    }
}
