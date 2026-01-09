package com.mocktalkback.dev;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mocktalkback.domain.common.controller.HealthCheckController;
import com.mocktalkback.domain.role.type.AuthBits;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.global.auth.jwt.JwtAccessDeniedHandler;
import com.mocktalkback.global.auth.jwt.JwtAuthEntryPoint;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.config.SecurityConfig;

@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "JWT_SECRET=abcdefghijklmnopqrstuvwxyz012345",
        "JWT_ISSUER=test-issuer",
        "server.port=0"
})
@WebMvcTest(controllers = { TestAuthController.class, HealthCheckController.class })
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        JwtAuthEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class TestAuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 헬스 엔드포인트는 인증 없이 GET 요청이 가능해야 한다.
    @Test
    void health_endpoint_allows_unauthenticated_get() throws Exception {
        // Given
        var request = get("/health");

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isOk());
    }

    // 헬스 엔드포인트는 지원하지 않는 HTTP 메서드를 거부해야 한다.
    @Test
    void health_endpoint_rejects_post_method() throws Exception {
        // Given
        var request = post("/health");

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isMethodNotAllowed());
    }

    // 사용자 엔드포인트는 인증이 필요해야 한다.
    @Test
    void user_endpoint_requires_authentication() throws Exception {
        // Given
        var request = get("/api/dev/user");

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isUnauthorized());
    }

    // 사용자 엔드포인트는 USER 권한을 허용해야 한다.
    @Test
    void user_endpoint_allows_user_role() throws Exception {
        // Given
        String token = bearerToken(RoleNames.USER, AuthBits.READ);
        var request = post("/api/dev/user")
                .header(HttpHeaders.AUTHORIZATION, token);

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isOk());
    }

    // 사용자 엔드포인트는 지원하지 않는 HTTP 메서드를 거부해야 한다.
    @Test
    void user_endpoint_rejects_get_method() throws Exception {
        // Given
        String token = bearerToken(RoleNames.USER, AuthBits.READ);
        var request = get("/api/dev/user")
                .header(HttpHeaders.AUTHORIZATION, token);

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isMethodNotAllowed());
    }

    // 관리자 엔드포인트는 USER 권한을 거부해야 한다.
    @Test
    void admin_endpoint_rejects_user_role() throws Exception {
        // Given
        String token = bearerToken(RoleNames.USER, AuthBits.READ);
        var request = post("/api/dev/admin")
                .header(HttpHeaders.AUTHORIZATION, token);

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isForbidden());
    }

    // 관리자 엔드포인트는 ADMIN 권한을 허용해야 한다.
    @Test
    void admin_endpoint_allows_admin_role() throws Exception {
        // Given
        int adminBits = AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE | AuthBits.ADMIN;
        String token = bearerToken(RoleNames.ADMIN, adminBits);
        var request = post("/api/dev/admin")
                .header(HttpHeaders.AUTHORIZATION, token);

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isOk());
    }

    // 관리자 엔드포인트는 지원하지 않는 HTTP 메서드를 거부해야 한다.
    @Test
    void admin_endpoint_rejects_get_method() throws Exception {
        // Given
        int adminBits = AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE | AuthBits.ADMIN;
        String token = bearerToken(RoleNames.ADMIN, adminBits);
        var request = get("/api/dev/admin")
                .header(HttpHeaders.AUTHORIZATION, token);

        // When
        var result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isMethodNotAllowed());
    }

    private String bearerToken(String roleName, int authBit) {
        return "Bearer " + jwtTokenProvider.createAccessToken(1L, roleName, authBit);
    }
}
