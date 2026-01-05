package com.mocktalkback.domain.user.controller;

import com.mocktalkback.domain.user.controller.dto.LoginRequest;
import com.mocktalkback.domain.user.controller.dto.RegisterRequest;
import com.mocktalkback.domain.user.controller.dto.TokenResponse;
import com.mocktalkback.domain.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid RegisterRequest req) {
        authService.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest req) {
        return authService.login(req);
    }
}
