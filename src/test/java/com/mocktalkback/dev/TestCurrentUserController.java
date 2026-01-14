package com.mocktalkback.dev;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestCurrentUserController {

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/dev/me")
    ResponseEntity<UserSummary> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("NONE");
        return ResponseEntity.ok(new UserSummary(userId, role));
    }

    record UserSummary(Long userId, String role) {}
}
