package com.traditionalmarket.backend.store.exception;

public class StoreNotFoundException extends RuntimeException {

    public StoreNotFoundException(Long marketId, Long storeId) {
        super("점포를 찾을 수 없습니다. marketId=" + marketId + ", storeId=" + storeId);
    }
}
