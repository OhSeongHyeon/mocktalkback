package com.mocktalkback.global.common.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.Getter;

@Getter
public class ApiError {

    private final String code;      // 예: AUTH_001, USER_404
    private final String reason;    // 사용자에게 보여줄 메시지(짧게)
    private final String path;      // 요청 경로
    private final OffsetDateTime timestamp;

    private final Map<String, Object> details; // 필드 에러, 추가 정보(선택)

    private ApiError(String code, String reason, String path, OffsetDateTime timestamp, Map<String, Object> details) {
        this.code = code;
        this.reason = reason;
        this.path = path;
        this.timestamp = timestamp;
        this.details = details;
    }

    public static ApiError of(ErrorCode errorCode, String path) {
        return of(errorCode, null, path, null);
    }

    public static ApiError of(ErrorCode errorCode, String reason, String path, Map<String, Object> details) {
        return new ApiError(errorCode.getCode(), resolveReason(errorCode, reason), path, OffsetDateTime.now(), details);
    }

    public static ApiError of(String code, String reason, String path, Map<String, Object> details) {
        return new ApiError(code, reason, path, OffsetDateTime.now(), details);
    }

    private static String resolveReason(ErrorCode errorCode, String reason) {
        if (reason == null || reason.isBlank()) {
            return errorCode.getDefaultMessage();
        }
        return reason;
    }

}

