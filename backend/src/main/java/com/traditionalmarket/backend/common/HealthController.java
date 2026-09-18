package com.traditionalmarket.backend.common;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "시스템", description = "서비스 상태 확인 API")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "서비스 상태 확인")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", "traditional-market-backend");
    }
}
