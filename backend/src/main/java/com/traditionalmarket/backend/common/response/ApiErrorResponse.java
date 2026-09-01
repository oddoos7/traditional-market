package com.traditionalmarket.backend.common.response;

public record ApiErrorResponse(
        String code,
        String message
) {
}
