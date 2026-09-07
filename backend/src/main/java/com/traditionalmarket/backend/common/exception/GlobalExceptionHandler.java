package com.traditionalmarket.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.traditionalmarket.backend.common.response.ApiErrorResponse;
import com.traditionalmarket.backend.market.exception.MarketNotFoundException;
import com.traditionalmarket.backend.review.exception.DuplicateReviewException;
import com.traditionalmarket.backend.review.exception.ReviewNotFoundException;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.user.exception.DuplicateEmailException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateReview(
            DuplicateReviewException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("REVIEW_ALREADY_EXISTS", exception.getMessage()));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReviewNotFound(
            ReviewNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("REVIEW_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("ACCESS_DENIED", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("EMAIL_ALREADY_EXISTS", exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));
    }

    @ExceptionHandler(MarketNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMarketNotFound(
            MarketNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("MARKET_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(StoreNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleStoreNotFound(
            StoreNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("STORE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return badRequest(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return badRequest(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return badRequest(exception.getName() + " 값이 올바르지 않습니다.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable() {
        return badRequest("요청 본문의 형식 또는 값이 올바르지 않습니다.");
    }

    private ResponseEntity<ApiErrorResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", message));
    }
}
