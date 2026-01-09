package com.mocktalkback.domain.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.AuthBits;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.dto.AuthTokens;
import com.mocktalkback.domain.user.dto.JoinRequest;
import com.mocktalkback.domain.user.dto.LoginRequest;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.domain.user.service.AuthService;
import com.mocktalkback.global.auth.CookieUtil;
import com.mocktalkback.global.auth.OriginAllowlistFilter;
import com.mocktalkback.global.auth.jwt.JwtAccessDeniedHandler;
import com.mocktalkback.global.auth.jwt.JwtAuthEntryPoint;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;
import com.mocktalkback.global.config.SecurityConfig;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthEntryPoint.class,
        JwtAccessDeniedHandler.class,
        CookieUtil.class,
        OriginAllowlistFilter.class
})
@TestPropertySource(properties = {
        "DEV_SERVER_PORT=0",
        "SECURITY_COOKIE_SECURE=false",
        "SECURITY_ORIGIN_ALLOWLIST=http://localhost:5173"
})
class AuthControllerTest {

    private static final String ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // 회원가입 API는 성공 응답을 반환해야 한다.
    @Test
    void join_returns_ok() throws Exception {
        // Given: 회원가입 요청
        JoinRequest req = new JoinRequest(
                "user01",
                "user01@example.com",
                "password12",
                "password12",
                null,
                null,
                null
        );

        // When: 회원가입 API 호출
        var result = mockMvc.perform(post("/api/auth/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(authService).join(any(JoinRequest.class));
    }

    // 로그인 API는 refresh 쿠키를 설정하고 access 토큰을 반환해야 한다.
    @Test
    void login_sets_refresh_cookie_and_returns_access_token() throws Exception {
        // Given: 로그인 요청과 토큰 응답
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthTokens("access-token", 3600, "refresh-token", 1200));

        LoginRequest req = new LoginRequest("user01", "password12");

        // When: 로그인 API 호출
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Then: refresh 쿠키와 access 토큰 확인
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // refresh 쿠키가 없으면 401과 함께 쿠키가 삭제되어야 한다.
    @Test
    void refresh_without_cookie_returns_unauthorized_and_clears_cookie() throws Exception {
        // Given: refresh 쿠키 없음

        // When: refresh API 호출
        var result = mockMvc.perform(post("/api/auth/refresh")
                .header("Origin", ORIGIN));

        // Then: 401 + 쿠키 삭제
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    // refresh 요청이 성공하면 쿠키 회전과 access 토큰 반환이 되어야 한다.
    @Test
    void refresh_rotates_cookie_and_returns_access_token() throws Exception {
        // Given: refresh 회전 성공과 사용자 조회
        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.Rotated(1L, "new-refresh", 600));

        UserEntity user = userWithRole(RoleNames.USER, AuthBits.READ);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L, RoleNames.USER, AuthBits.READ)).thenReturn("new-access");
        when(jwtTokenProvider.accessTtlSec()).thenReturn(3600L);

        // When: refresh API 호출
        var result = mockMvc.perform(post("/api/auth/refresh")
                .header("Origin", ORIGIN)
                .cookie(new Cookie(CookieUtil.COOKIE_NAME, "old-refresh")));

        // Then: refresh 쿠키 회전 + access 토큰 반환
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=new-refresh")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=600")))
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    // 잠긴 계정은 refresh 시 401 처리되고 세션이 폐기되어야 한다.
    @Test
    void refresh_with_locked_user_returns_unauthorized_and_revokes() throws Exception {
        // Given: 회전 성공했지만 사용자가 잠김
        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.Rotated(1L, "new-refresh", 600));
        doNothing().when(refreshTokenService).revoke("old-refresh");

        UserEntity user = userWithRole(RoleNames.USER, AuthBits.READ);
        ReflectionTestUtils.setField(user, "locked", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When: refresh API 호출
        var result = mockMvc.perform(post("/api/auth/refresh")
                .header("Origin", ORIGIN)
                .cookie(new Cookie(CookieUtil.COOKIE_NAME, "old-refresh")));

        // Then: 401 + 쿠키 삭제 + revoke 호출
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(refreshTokenService).revoke("old-refresh");
    }

    // logout은 refresh 세션을 폐기하고 쿠키를 삭제해야 한다.
    @Test
    void logout_revokes_and_clears_cookie() throws Exception {
        // Given: refresh 쿠키 보유
        doNothing().when(refreshTokenService).revoke("old-refresh");

        // When: logout API 호출
        var result = mockMvc.perform(post("/api/auth/logout")
                .header("Origin", ORIGIN)
                .cookie(new Cookie(CookieUtil.COOKIE_NAME, "old-refresh")));

        // Then: 쿠키 삭제 + revoke 호출
        result.andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(refreshTokenService).revoke("old-refresh");
    }

    private UserEntity userWithRole(String roleName, int authBit) {
        RoleEntity role = RoleEntity.create(roleName, authBit, "role");
        UserEntity user = UserEntity.createLocal(
                role,
                "loginId",
                "email@example.com",
                "pw",
                "userName",
                "displayName",
                "handle"
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
