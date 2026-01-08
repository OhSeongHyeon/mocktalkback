package com.mocktalkback.global.exception;

import com.mocktalkback.global.common.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 요청 바디 Bean Validation(@Valid) 검증 실패 처리.
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex
    ) {
        String message = extractFieldErrors(ex.getBindingResult());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    // 파라미터/경로/쿼리 @Validated 검증 실패 처리.
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        String message = extractConstraintViolations(ex.getConstraintViolations());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    // 경로/쿼리 파라미터 타입 불일치 처리.
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        String message = "Invalid parameter: " + ex.getName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    // 잘못된 JSON 등 요청 바디 파싱 실패 처리.
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("Malformed request body"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    // 지원하지 않는 HTTP 메서드 요청 처리.
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex
    ) {
        String message = "지원하지 않는 HTTP 메서드입니다: " + ex.getMethod();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(message));
    }

    @ExceptionHandler(AuthenticationException.class)
    // 인증 실패(미인증) 처리.
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Unauthorized"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    // 인가 실패(권한 부족) 처리.
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("Forbidden"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    // 서비스/컨트롤러에서 발생한 잘못된 인자 처리.
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    // 처리되지 않은 모든 예외의 최종 처리.
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal server error"));
    }

    private String extractFieldErrors(BindingResult result) {
        List<FieldError> errors = result.getFieldErrors();
        if (errors.isEmpty()) {
            return "Invalid request";
        }
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }

    private String extractConstraintViolations(
            java.util.Set<ConstraintViolation<?>> violations
    ) {
        if (violations.isEmpty()) {
            return "Invalid request";
        }
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
    }
}
