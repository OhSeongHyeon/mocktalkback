package com.mocktalkback.domain.user.controller;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mocktalkback.domain.user.dto.AuthTokens;
import com.mocktalkback.domain.user.dto.AccessTokenResult;
import com.mocktalkback.domain.user.dto.JoinRequest;
import com.mocktalkback.domain.user.dto.LoginRequest;
import com.mocktalkback.domain.user.dto.OAuth2CodeRequest;
import com.mocktalkback.domain.user.dto.RefreshTokens;
import com.mocktalkback.domain.user.dto.TokenResponse;
import com.mocktalkback.domain.user.service.AuthService;
import com.mocktalkback.global.auth.CookieUtil;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;
import com.mocktalkback.global.common.ApiResponse;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService userService;
    private final CookieUtil cookieUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> join(@Valid @RequestBody JoinRequest joinDto) {
        userService.join(joinDto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        AuthTokens tokens = userService.login(req);
        ResponseCookie cookie = req.rememberMe()
                ? cookieUtil.create(tokens.refreshToken(), tokens.refreshExpiresInSec())
                : cookieUtil.createSession(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresInSec()));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(value = CookieUtil.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                .build();
        }

        try {
            RefreshTokens tokens = userService.refresh(refreshToken);

            // 새 refresh 쿠키로 회전
            ResponseCookie cookie = tokens.rememberMe()
                    ? cookieUtil.create(tokens.refreshToken(), tokens.refreshExpiresInSec())
                    : cookieUtil.createSession(tokens.refreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresInSec()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = CookieUtil.COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                refreshTokenService.revoke(refreshToken);
            } catch (ResponseStatusException ignored) {
            }
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                .build();
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<TokenResponse> oauth2Callback(@RequestBody @Valid OAuth2CodeRequest req) {
        AccessTokenResult result = userService.exchangeOAuth2Code(req.code());
        return ResponseEntity.ok(new TokenResponse(
                result.accessToken(),
                "Bearer",
                result.accessExpiresInSec()
        ));
    }

}
