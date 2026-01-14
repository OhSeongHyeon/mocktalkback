package com.mocktalkback.global.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof Integer) {
            return ((Integer) principal).longValue();
        }
        if (principal instanceof String) {
            String value = (String) principal;
            if ("anonymousUser".equals(value)) {
                throw new IllegalStateException("인증 정보가 없습니다.");
            }
            return Long.valueOf(value);
        }

        throw new IllegalStateException("사용자 식별자를 확인할 수 없습니다.");
    }
}
