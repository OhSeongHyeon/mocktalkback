package com.mocktalkback.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.user.controller.dto.LoginRequest;
import com.mocktalkback.domain.user.controller.dto.RegisterRequest;
import com.mocktalkback.domain.user.controller.dto.TokenResponse;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.domain.user.service.AuthService;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.auth.jwt.RefreshCookie;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookie refreshCookie;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, RefreshCookie refreshCookie, RefreshTokenService refreshTokenService,
            UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.refreshCookie = refreshCookie;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid RegisterRequest req) {
        authService.register(req);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        var tokens = authService.login(req);

        var cookie = refreshCookie.create(tokens.refreshToken(), tokens.refreshExpiresInSec());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresInSec()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = RefreshCookie.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(value = RefreshCookie.COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<TokenResponse>build();
        }

        var rotated = refreshTokenService.rotate(refreshToken);
        var cookie = refreshCookie.create(rotated.refreshToken(), jwtTokenProvider.refreshTtlSec());

        var body = authService.refreshAccess(rotated.userId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }


}
