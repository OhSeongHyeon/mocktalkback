package com.mocktalkback.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@RestController
public class OAuth2TestController {

    @GetMapping("/auth/oauth2/success")
    public ResponseEntity<?> success(HttpServletRequest request) {
        // 브라우저로 들어오면 쿠키가 저장된 상태인지 확인용
        Cookie[] cookies = request.getCookies();
        String refresh = (cookies == null) ? null :
                Arrays.stream(cookies)
                        .filter(c -> "refresh_token".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);

        return ResponseEntity.ok().body(
                refresh == null ? "NO refresh cookie" : "OK (refresh cookie exists)"
        );
    }
}
