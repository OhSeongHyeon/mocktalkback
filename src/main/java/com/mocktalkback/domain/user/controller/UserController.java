package com.mocktalkback.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.user.dto.JoinRequest;
import com.mocktalkback.domain.user.dto.LoginRequest;
import com.mocktalkback.domain.user.dto.TokenResponse;
import com.mocktalkback.domain.user.service.UserService;
import com.mocktalkback.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> join(@Valid @RequestBody JoinRequest joinDto) {
        userService.join(joinDto);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest req) {
        return userService.login(req);
    }
}
