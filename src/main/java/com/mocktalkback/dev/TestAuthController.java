package com.mocktalkback.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.global.common.ApiEnvelope;

import lombok.RequiredArgsConstructor;

@Profile("dev")
@RequiredArgsConstructor
@RestController
public class TestAuthController {

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/dev/user")
    public ResponseEntity<ApiEnvelope<String>> userOnly() {
        return ResponseEntity.ok(ApiEnvelope.ok("user"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/dev/admin")
    public ResponseEntity<ApiEnvelope<String>> healthCheck() {
        return ResponseEntity.ok(ApiEnvelope.ok("admin"));
    }

}
