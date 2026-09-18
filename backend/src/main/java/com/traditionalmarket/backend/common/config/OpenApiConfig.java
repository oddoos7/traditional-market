package com.traditionalmarket.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI traditionalMarketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("전통시장 API")
                        .version("v1")
                        .description("전통시장, 점포, 상품, 장바구니, 주문 및 결제 기능을 제공하는 REST API입니다. "
                                + "로그인 기반 API는 JSESSIONID 세션 쿠키를 사용하며, 데이터 변경 요청에는 "
                                + "GET /api/auth/csrf에서 발급받은 CSRF 토큰이 필요합니다.")
                        .contact(new Contact()
                                .name("traditional-market")
                                .url("https://github.com/oddoos7/traditional-market")));
    }
}
