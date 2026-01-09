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
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.user.dto.AuthTokens;
import com.mocktalkback.domain.user.dto.JoinRequest;
import com.mocktalkback.domain.user.dto.LoginRequest;
import com.mocktalkback.domain.user.dto.TokenResponse;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.domain.user.service.AuthService;
import com.mocktalkback.global.auth.CookieUtil;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;
import com.mocktalkback.global.auth.jwt.RefreshTokenService.Rotated;
import com.mocktalkback.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService userService;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> join(@Valid @RequestBody JoinRequest joinDto) {
        userService.join(joinDto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        AuthTokens tokens = userService.login(req);
        ResponseCookie cookie = cookieUtil.create(tokens.refreshToken(), tokens.refreshExpiresInSec());
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
            Rotated rotated = refreshTokenService.rotate(refreshToken);

            UserEntity user = userRepository.findById(rotated.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            if (!user.isEnabled() || user.isLocked()) {
                try {
                    refreshTokenService.revoke(refreshToken);
                } catch (ResponseStatusException ignored) {
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                        .build();
            }

            // 새 refresh 쿠키로 회전
            ResponseCookie cookie = cookieUtil.create(rotated.refreshToken(), rotated.refreshExpiresInSec());

            String access = jwtTokenProvider.createAccessToken(
                    user.getId(),
                    user.getRole().getRoleName(),
                    user.getRole().getAuthBit());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenResponse(access, "Bearer", jwtTokenProvider.accessTtlSec()));
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

}
