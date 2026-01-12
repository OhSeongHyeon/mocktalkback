package com.mocktalkback.dev;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
class TestUserRootRoleController {

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/dev/user")
    ResponseEntity<String> userOnly() {
        return ResponseEntity.ok("user");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/dev/admin")
    ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("admin");
    }
}
