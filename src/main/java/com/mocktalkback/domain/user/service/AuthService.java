package com.mocktalkback.domain.user.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.dto.AuthTokens;
import com.mocktalkback.domain.user.dto.AccessTokenResult;
import com.mocktalkback.domain.user.dto.JoinRequest;
import com.mocktalkback.domain.user.dto.LoginRequest;
import com.mocktalkback.domain.user.dto.RefreshTokens;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;
import com.mocktalkback.global.auth.jwt.RefreshTokenService.Rotated;
import com.mocktalkback.global.auth.oauth2.OAuth2CodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int HANDLE_LENGTH = 12;
    private static final int HANDLE_TRIES = 10;
    private static final char[] HANDLE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom HANDLE_RANDOM = new SecureRandom();
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2CodeService oAuth2CodeService;
    
    @Transactional
    public void join(JoinRequest joinDto) {
        if (!joinDto.password().equals(joinDto.confirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String loginId = joinDto.loginId().trim();
        if (!StringUtils.hasText(loginId)) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (userRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        String email = joinDto.email().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String userName = resolveOptional(joinDto.userName(), loginId);
        String displayName = resolveOptional(joinDto.displayName(), userName);

        requireMaxLength(userName, 32, "사용자명");
        requireMaxLength(displayName, 16, "표시명");

        String handle;
        if (StringUtils.hasText(joinDto.handle())) {
            handle = joinDto.handle().trim();
            requireMaxLength(handle, 24, "핸들");
            if (userRepository.existsByHandle(handle)) {
                throw new IllegalArgumentException("이미 사용 중인 핸들입니다.");
            }
        } else {
            handle = generateUniqueHandle();
        }

        RoleEntity role = roleRepository.findByRoleName(RoleNames.USER)
                .orElseThrow(() -> new IllegalStateException("기본 권한이 없습니다."));

        String encodedPw = passwordEncoder.encode(joinDto.password());
        UserEntity user = UserEntity.createLocal(
                role,
                loginId,
                email,
                encodedPw,
                userName,
                displayName,
                handle
        );

        userRepository.save(user);
    }

    private String resolveOptional(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private void requireMaxLength(String value, int max, String fieldName) {
        if (value.length() > max) {
            throw new IllegalArgumentException(fieldName + "은 " + max + "자 이하이어야 합니다.");
        }
    }

    private String generateUniqueHandle() {
        for (int i = 0; i < HANDLE_TRIES; i++) {
            String candidate = randomHandle(HANDLE_LENGTH);
            if (!userRepository.existsByHandle(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("사용 가능한 핸들을 생성하지 못했습니다.");
    }

    private String randomHandle(int length) {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = HANDLE_CHARS[HANDLE_RANDOM.nextInt(HANDLE_CHARS.length)];
        }
        return new String(buffer);
    }

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest req) {
        UserEntity u = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!u.isEnabled() || u.isLocked()) {
            throw new IllegalStateException("계정이 비활성화/잠금 상태입니다.");
        }

        if (!passwordEncoder.matches(req.password(), u.getPwHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwt.createAccessToken(
                u.getId(),
                u.getRole().getRoleName(),
                u.getRole().getAuthBit()
        );

        RefreshTokenService.IssuedRefresh issued = refreshTokenService.issue(u.getId(), req.rememberMe());

        return new AuthTokens(token, jwt.accessTtlSec(), issued.refreshToken(), issued.refreshExpiresInSec());
    }

    @Transactional(readOnly = true)
    public RefreshTokens refresh(String refreshToken) {
        Rotated rotated = refreshTokenService.rotate(refreshToken);

        UserEntity user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled() || user.isLocked()) {
            try {
                refreshTokenService.revoke(refreshToken);
            } catch (ResponseStatusException ignored) {
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String access = jwt.createAccessToken(
                user.getId(),
                user.getRole().getRoleName(),
                user.getRole().getAuthBit()
        );

        return new RefreshTokens(
                access,
                jwt.accessTtlSec(),
                rotated.refreshToken(),
                rotated.refreshExpiresInSec(),
                rotated.rememberMe()
        );
    }

    @Transactional(readOnly = true)
    public AccessTokenResult exchangeOAuth2Code(String code) {
        Long userId = oAuth2CodeService.consume(code);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAUTH2_CODE_INVALID");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled() || user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String access = jwt.createAccessToken(
                user.getId(),
                user.getRole().getRoleName(),
                user.getRole().getAuthBit()
        );

        return new AccessTokenResult(access, jwt.accessTtlSec());
    }

}
