package com.mocktalkback.global.exception;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.ApiError;
import com.mocktalkback.global.common.dto.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 요청 바디 Bean Validation(@Valid) 검증 실패 처리.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = extractFieldErrorDetails(ex.getBindingResult());
        return buildError(ErrorCode.COMMON_BAD_REQUEST, request, null, details);
    }

    // 파라미터/경로/쿼리 @Validated 검증 실패 처리.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> details = extractConstraintViolationDetails(ex.getConstraintViolations());
        return buildError(ErrorCode.COMMON_BAD_REQUEST, request, null, details);
    }

    // 경로/쿼리 파라미터 타입 불일치 처리.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid parameter: " + ex.getName();
        return buildError(ErrorCode.COMMON_BAD_REQUEST, request, message, null);
    }

    // 잘못된 JSON 등 요청 바디 파싱 실패 처리.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(ErrorCode.COMMON_BAD_REQUEST, request, "Malformed request body", null);
    }

    // 지원하지 않는 HTTP 메서드 요청 처리.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = "지원하지 않는 HTTP 메서드입니다: " + ex.getMethod();
        return buildError(ErrorCode.COMMON_METHOD_NOT_ALLOWED, request, message, null);
    }

    // 인증 실패(미인증) 처리.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildError(ErrorCode.COMMON_UNAUTHORIZED, request, null, null);
    }

    // 인가 실패(권한 부족) 처리.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildError(ErrorCode.COMMON_FORBIDDEN, request, null, null);
    }

    // 서비스/컨트롤러에서 발생한 잘못된 인자 처리.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(ErrorCode.COMMON_BAD_REQUEST, request, ex.getMessage(), null);
    }

    // 처리되지 않은 모든 예외의 최종 처리.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return buildError(ErrorCode.COMMON_INTERNAL_ERROR, request, null, null);
    }

    private Map<String, Object> extractFieldErrorDetails(BindingResult result) {
        List<FieldError> errors = result.getFieldErrors();
        List<Map<String, String>> fieldErrors = errors.stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ))
                .collect(Collectors.toList());
        return Map.of("fieldErrors", fieldErrors);
    }

    private Map<String, Object> extractConstraintViolationDetails(
            Set<ConstraintViolation<?>> violations
    ) {
        List<Map<String, String>> violationErrors = violations.stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage()
                ))
                .collect(Collectors.toList());
        return Map.of("violations", violationErrors);
    }

    private ResponseEntity<ApiEnvelope<Void>> buildError(ErrorCode errorCode, HttpServletRequest request, String reason, Map<String, Object> details) {
        String path = request != null ? request.getRequestURI() : "";
        ApiError error = ApiError.of(errorCode, reason, path, details);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiEnvelope.fail(error));
    }

}
