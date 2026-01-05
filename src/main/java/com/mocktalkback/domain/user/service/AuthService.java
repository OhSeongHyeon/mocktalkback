package com.mocktalkback.domain.user.service;

import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.controller.dto.LoginRequest;
import com.mocktalkback.domain.user.controller.dto.RegisterRequest;
import com.mocktalkback.domain.user.controller.dto.TokenResponse;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwt
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        var role = roleRepository.findByRoleName(RoleNames.USER)
                .orElseThrow(() -> new IllegalStateException("USER 역할이 없습니다. seed 확인"));

        String handle = resolveHandle(req);

        UserEntity user = UserEntity.createLocal(
                role,
                req.email(),
                passwordEncoder.encode(req.password()),
                req.userName(),
                req.displayName(),
                handle);

        userRepository.save(user);
    }
    
    private String resolveHandle(RegisterRequest req) {
        // 1) 사용자가 입력한 handle이 있으면 그걸 우선 사용
        String input = req.handle();
        if (input != null && !input.isBlank()) {
            String handle = input.trim();
            if (userRepository.existsByHandle(handle)) {
                throw new IllegalArgumentException("이미 사용 중인 handle 입니다.");
            }
            return handle;
        }

        // 2) 비어있으면 자동 생성
        String base = HandleGenerator.baseFrom(req.displayName(), req.userName());
        return generateUniqueHandle(base);
    }

    private String generateUniqueHandle(String base) {
        // base 자체가 비어있지 않도록 HandleGenerator에서 최소 "user" 같은 값 보장한다고 가정
        String handle = truncateTo24(base);

        int tries = 0;
        while (userRepository.existsByHandle(handle)) {
            if (++tries > 20) {
                throw new IllegalStateException("handle 생성 실패(재시도 초과)");
            }
            handle = truncateTo24(base + HandleGenerator.randomSuffix());
        }
        return handle;
    }

    private String truncateTo24(String s) {
        if (s == null) return null;
        return (s.length() <= 24) ? s : s.substring(0, 24);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        UserEntity u = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!u.isEnabled() || u.isLocked()) {
            throw new IllegalStateException("계정이 비활성화/잠금 상태입니다.");
        }

        if (!passwordEncoder.matches(req.password(), u.getPwHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwt.createAccessToken(
                u.getId(),
                u.getEmail(),
                u.getRole().getRoleName(),
                u.getRole().getAuthBit()
        );

        return new TokenResponse(token, "Bearer", jwt.accessTtlSec());
    }
}
