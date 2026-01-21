package com.mocktalkback.global.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청 메타데이터(IP, User-Agent) 추출 유틸리티.
 */
public final class RequestMetadataResolver {
    private RequestMetadataResolver() {}

    /**
     * 프록시 환경을 고려하여 클라이언트 IP를 추출한다.
     */
    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ipFromForwardedFor = extractFirstIp(forwardedFor);
        if (isPresent(ipFromForwardedFor)) {
            return ipFromForwardedFor;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (isPresent(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * User-Agent 헤더를 반환한다.
     */
    public static String resolveUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private static String extractFirstIp(String headerValue) {
        if (!isPresent(headerValue)) {
            return null;
        }
        String[] parts = headerValue.split(",");
        if (parts.length == 0) {
            return null;
        }
        String candidate = parts[0].trim();
        if (!isPresent(candidate)) {
            return null;
        }
        return "unknown".equalsIgnoreCase(candidate) ? null : candidate;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
