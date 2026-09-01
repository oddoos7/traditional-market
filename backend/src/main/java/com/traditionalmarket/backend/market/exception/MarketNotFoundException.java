package com.traditionalmarket.backend.market.exception;

public class MarketNotFoundException extends RuntimeException {

    public MarketNotFoundException(Long marketId) {
        super("시장을 찾을 수 없습니다. marketId=" + marketId);
    }
}
