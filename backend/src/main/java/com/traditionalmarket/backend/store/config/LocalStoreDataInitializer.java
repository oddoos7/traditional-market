package com.traditionalmarket.backend.store.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.repository.MarketRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.domain.StoreCategory;
import com.traditionalmarket.backend.store.domain.StoreStatus;
import com.traditionalmarket.backend.store.repository.StoreRepository;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalStoreDataInitializer {

    @Bean
    @Order(2)
    CommandLineRunner initializeLocalStores(
            MarketRepository marketRepository,
            StoreRepository storeRepository
    ) {
        return args -> {
            if (storeRepository.count() > 0) {
                return;
            }

            Map<String, Market> markets = marketRepository.findAll().stream()
                    .collect(Collectors.toMap(Market::getName, Function.identity()));

            Market centralMarket = getMarket(markets, "우리동네 중앙시장");
            Market alleyMarket = getMarket(markets, "푸른골목시장");
            Market hanbitMarket = getMarket(markets, "한빛전통시장");

            storeRepository.saveAll(List.of(
                    Store.builder()
                            .market(centralMarket)
                            .name("행복청과")
                            .description("매일 새벽 들어오는 제철 과일과 채소를 판매합니다.")
                            .category(StoreCategory.AGRICULTURAL)
                            .location("가동 12호")
                            .phone("02-1000-2001")
                            .openingHours("08:00 ~ 19:00")
                            .closedDays("매월 둘째 주 일요일")
                            .status(StoreStatus.OPEN)
                            .build(),
                    Store.builder()
                            .market(centralMarket)
                            .name("중앙반찬")
                            .description("매일 직접 만드는 반찬과 국을 만날 수 있는 점포입니다.")
                            .category(StoreCategory.FOOD)
                            .location("나동 5호")
                            .phone("02-1000-2002")
                            .openingHours("09:00 ~ 20:00")
                            .closedDays("매주 월요일")
                            .status(StoreStatus.OPEN)
                            .build(),
                    Store.builder()
                            .market(centralMarket)
                            .name("정다운수산")
                            .description("손질한 생선과 제철 해산물을 판매합니다.")
                            .category(StoreCategory.SEAFOOD)
                            .location("수산동 3호")
                            .phone("02-1000-2003")
                            .openingHours("07:30 ~ 18:30")
                            .closedDays("임시 휴무 중")
                            .status(StoreStatus.TEMPORARILY_CLOSED)
                            .build(),
                    Store.builder()
                            .market(alleyMarket)
                            .name("푸른떡방")
                            .description("당일 만든 떡과 전통 간식을 판매합니다.")
                            .category(StoreCategory.FOOD)
                            .location("골목 8호")
                            .phone("02-2000-3001")
                            .openingHours("08:00 ~ 18:00")
                            .closedDays("매주 월요일")
                            .status(StoreStatus.OPEN)
                            .build(),
                    Store.builder()
                            .market(alleyMarket)
                            .name("살림그릇")
                            .description("주방용품과 생활 잡화를 판매하는 점포입니다.")
                            .category(StoreCategory.HOUSEHOLD)
                            .location("골목 15호")
                            .phone("02-2000-3002")
                            .openingHours("10:00 ~ 19:00")
                            .closedDays("매주 월요일")
                            .status(StoreStatus.OPEN)
                            .build(),
                    Store.builder()
                            .market(hanbitMarket)
                            .name("한빛국수")
                            .description("따뜻한 멸치국수와 비빔국수를 맛볼 수 있습니다.")
                            .category(StoreCategory.RESTAURANT)
                            .location("먹거리동 2호")
                            .phone("02-3000-4001")
                            .openingHours("10:30 ~ 17:30")
                            .closedDays("시장 운영 재개 시까지")
                            .status(StoreStatus.TEMPORARILY_CLOSED)
                            .build()
            ));
        };
    }

    private Market getMarket(Map<String, Market> markets, String marketName) {
        Market market = markets.get(marketName);
        if (market == null) {
            throw new IllegalStateException("로컬 점포 데이터에 필요한 시장이 없습니다. marketName=" + marketName);
        }
        return market;
    }
}
