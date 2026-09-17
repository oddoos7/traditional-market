package com.traditionalmarket.backend.product.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import com.traditionalmarket.backend.product.domain.Product;
import com.traditionalmarket.backend.product.domain.ProductStatus;
import com.traditionalmarket.backend.product.repository.ProductRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.repository.StoreRepository;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalProductDataInitializer {

    @Bean
    @Order(3)
    CommandLineRunner initializeLocalProducts(
            StoreRepository storeRepository,
            ProductRepository productRepository
    ) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            Map<String, Store> stores = storeRepository.findAll().stream()
                    .collect(Collectors.toMap(Store::getName, Function.identity()));

            Store fruitStore = getStore(stores, "행복청과");
            Store sideDishStore = getStore(stores, "중앙반찬");
            Store seafoodStore = getStore(stores, "정다운수산");
            Store riceCakeStore = getStore(stores, "푸른떡방");
            Store householdStore = getStore(stores, "살림그릇");
            Store noodleStore = getStore(stores, "한빛국수");

            productRepository.saveAll(List.of(
                    product(fruitStore, "새콤달콤 사과 1kg", "아삭하고 달콤한 제철 사과입니다.", "8500", 30, ProductStatus.ON_SALE),
                    product(fruitStore, "제철 딸기 500g", "당일 입고한 향긋한 딸기입니다.", "12000", 0, ProductStatus.SOLD_OUT),
                    product(sideDishStore, "수제 잡채 500g", "매일 아침 직접 만드는 잡채입니다.", "7000", 12, ProductStatus.ON_SALE),
                    product(sideDishStore, "포기김치 1kg", "국내산 재료로 담근 포기김치입니다.", "9500", 20, ProductStatus.ON_SALE),
                    product(seafoodStore, "손질 고등어 2마리", "구이용으로 손질한 신선한 고등어입니다.", "9000", 8, ProductStatus.ON_SALE),
                    product(riceCakeStore, "오늘 만든 모둠떡", "인절미와 절편을 담은 모둠 구성입니다.", "10000", 15, ProductStatus.ON_SALE),
                    product(householdStore, "스테인리스 채반", "튼튼하고 세척이 편리한 주방용 채반입니다.", "15000", 6, ProductStatus.ON_SALE),
                    product(noodleStore, "멸치국수 밀키트", "담백한 육수와 생면으로 구성한 밀키트입니다.", "8000", 10, ProductStatus.HIDDEN)
            ));
        };
    }

    private Product product(
            Store store,
            String name,
            String description,
            String price,
            int stockQuantity,
            ProductStatus status
    ) {
        return Product.builder()
                .store(store)
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stockQuantity(stockQuantity)
                .status(status)
                .build();
    }

    private Store getStore(Map<String, Store> stores, String storeName) {
        Store store = stores.get(storeName);
        if (store == null) {
            throw new IllegalStateException("로컬 상품 데이터에 필요한 점포가 없습니다. storeName=" + storeName);
        }
        return store;
    }
}
