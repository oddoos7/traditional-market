package com.traditionalmarket.backend.market.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import com.traditionalmarket.backend.market.domain.Market;
import com.traditionalmarket.backend.market.domain.MarketStatus;
import com.traditionalmarket.backend.market.repository.MarketRepository;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalMarketDataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initializeLocalMarkets(MarketRepository marketRepository) {
        return args -> {
            if (marketRepository.count() > 0) {
                return;
            }

            marketRepository.saveAll(List.of(
                    Market.builder()
                            .name("우리동네 중앙시장")
                            .description("신선한 식재료와 다양한 먹거리를 만날 수 있는 전통시장입니다.")
                            .address("서울특별시 행복구 중앙시장길 12")
                            .phone("02-0000-1001")
                            .openingHours("09:00 ~ 20:00")
                            .closedDays("매월 둘째 주 일요일")
                            .latitude(new BigDecimal("37.5665000"))
                            .longitude(new BigDecimal("126.9780000"))
                            .status(MarketStatus.OPEN)
                            .build(),
                    Market.builder()
                            .name("푸른골목시장")
                            .description("동네 상인들의 정겨운 분위기를 느낄 수 있는 골목형 시장입니다.")
                            .address("서울특별시 행복구 푸른길 35")
                            .phone("02-0000-1002")
                            .openingHours("10:00 ~ 19:00")
                            .closedDays("매주 월요일")
                            .latitude(new BigDecimal("37.5700000"))
                            .longitude(new BigDecimal("126.9820000"))
                            .status(MarketStatus.OPEN)
                            .build(),
                    Market.builder()
                            .name("한빛전통시장")
                            .description("지역 특산품과 수제 먹거리를 중심으로 운영되는 시장입니다.")
                            .address("서울특별시 한빛구 시장로 88")
                            .phone("02-0000-1003")
                            .openingHours("09:30 ~ 18:30")
                            .closedDays("임시 휴무 중")
                            .latitude(new BigDecimal("37.5600000"))
                            .longitude(new BigDecimal("126.9700000"))
                            .status(MarketStatus.TEMPORARILY_CLOSED)
                            .build()
            ));
        };
    }
}
