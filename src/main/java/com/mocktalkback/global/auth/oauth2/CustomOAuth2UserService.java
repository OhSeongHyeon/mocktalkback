package com.mocktalkback.global.auth.oauth2;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.entity.UserOAuthLinkEntity;
import com.mocktalkback.domain.user.repository.UserOAuthLinkRepository;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.common.util.HandleGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final int LOGIN_TRIES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserOAuthLinkRepository userOAuthLinkRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HandleGenerator handleGenerator;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String provider = normalizeProvider(userRequest.getClientRegistration().getRegistrationId());
        if (!OAuth2ProviderType.isSupported(provider)) {
            throw authException("unsupported_provider", "지원하지 않는 소셜 로그인입니다.");
        }

        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerId = resolveProviderId(provider, attributes);
        String rawEmail = resolveEmail(attributes);
        String name = resolveName(attributes);
        String providerUserName = resolveProviderUserName(provider, attributes);

        UserEntity user = resolveOrCreateUser(provider, providerId, rawEmail, name, providerUserName, attributes);
        if (!user.isEnabled() || user.isLocked()) {
            throw authException("user_disabled", "계정이 비활성화/잠금 상태입니다.");
        }

        Map<String, Object> merged = new HashMap<>(attributes);
        merged.put("userId", String.valueOf(user.getId()));
        merged.put("role", user.getRole().getRoleName());
        merged.put("authBit", user.getRole().getAuthBit());
        merged.put("provider", provider);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName())
        );

        return new DefaultOAuth2User(authorities, merged, "userId");
    }

    private UserEntity resolveOrCreateUser(
            String provider,
            String providerId,
            String rawEmail,
            String name,
            String providerUserName,
            Map<String, Object> attributes
    ) {
        UserOAuthLinkEntity existingLink = userOAuthLinkRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        if (existingLink != null) {
            return existingLink.getUser();
        }

        String handle = handleGenerator.generateUniqueHandle();
        int loginDigits = OAuth2ProviderType.GITHUB.equals(provider) ? 6 : 4;
        String loginId = generateUniqueLoginId(handle, loginDigits);
        String email = resolveEmailValue(provider, rawEmail, loginId);
        if (!StringUtils.hasText(rawEmail)) {
            for (int i = 0; i < LOGIN_TRIES; i++) {
                if (!userRepository.existsByEmail(email)) {
                    break;
                }
                loginId = generateUniqueLoginId(handle, loginDigits);
                email = resolveEmailValue(provider, rawEmail, loginId);
            }
            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException("사용 가능한 이메일을 생성하지 못했습니다.");
            }
        }

        UserEntity user = null;
        if (StringUtils.hasText(email)) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null && userOAuthLinkRepository.existsByUserAndProvider(user, provider)) {
                throw authException("provider_already_linked", "이미 연결된 소셜 계정입니다.");
            }
        }

        if (user == null) {
            String userName = resolveUserName(provider, providerUserName);
            String displayName = resolveDisplayName(provider, name, loginId, userName);
            boolean emailVerified = resolveEmailVerified(provider, attributes);

            RoleEntity role = roleRepository.findByRoleName(RoleNames.WRITER)
                    .orElseThrow(() -> new IllegalStateException("기본 권한이 없습니다."));

            String randomPw = UUID.randomUUID().toString();
            String encodedPw = passwordEncoder.encode(randomPw);

            user = UserEntity.createOAuth(
                    role,
                    loginId,
                    email,
                    encodedPw,
                    userName,
                    displayName,
                    handle,
                    emailVerified
            );
            userRepository.save(user);
        }

        UserOAuthLinkEntity link = UserOAuthLinkEntity.link(user, provider, providerId, email);
        userOAuthLinkRepository.save(link);

        return user;
    }

    private String normalizeProvider(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            throw authException("invalid_provider", "provider가 없습니다.");
        }
        return registrationId.trim().toLowerCase();
    }

    private String resolveProviderId(String provider, Map<String, Object> attributes) {
        Object raw;
        if (OAuth2ProviderType.GOOGLE.equals(provider)) {
            raw = attributes.get("sub");
        } else if (OAuth2ProviderType.GITHUB.equals(provider)) {
            raw = attributes.get("id");
        } else {
            raw = null;
        }
        String providerId = asString(raw);
        if (!StringUtils.hasText(providerId)) {
            throw authException("missing_provider_id", "providerId가 없습니다.");
        }
        return providerId.trim();
    }

    private String resolveEmail(Map<String, Object> attributes) {
        return asString(attributes.get("email"));
    }

    private String resolveName(Map<String, Object> attributes) {
        return asString(attributes.get("name"));
    }

    private String resolveProviderUserName(String provider, Map<String, Object> attributes) {
        if (OAuth2ProviderType.GITHUB.equals(provider)) {
            return asString(attributes.get("login"));
        }
        return null;
    }

    private String resolveEmailValue(String provider, String rawEmail, String loginId) {
        if (StringUtils.hasText(rawEmail)) {
            return rawEmail.trim();
        }
        String fallback = loginId + "@unknown.unknown";
        if (OAuth2ProviderType.GITHUB.equals(provider)) {
            return fallback;
        }
        return fallback;
    }

    private String resolveUserName(String provider, String providerUserName) {
        String fallback = OAuth2ProviderType.GOOGLE.equals(provider) ? "google_user" : "github_user";
        return normalizeValue(providerUserName, fallback, 32);
    }

    private String resolveDisplayName(String provider, String name, String loginId, String userName) {
        String fallback = OAuth2ProviderType.GITHUB.equals(provider) ? loginId : userName;
        return normalizeValue(name, fallback, 16);
    }

    private boolean resolveEmailVerified(String provider, Map<String, Object> attributes) {
        if (!OAuth2ProviderType.GOOGLE.equals(provider)) {
            return false;
        }
        Object raw = attributes.get("email_verified");
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    private String generateUniqueLoginId(String handle, int digits) {
        for (int i = 0; i < LOGIN_TRIES; i++) {
            String candidate = handle + randomDigits(digits);
            if (!userRepository.existsByLoginId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("사용 가능한 로그인 아이디를 생성하지 못했습니다.");
    }

    private String randomDigits(int digits) {
        StringBuilder builder = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private String normalizeValue(String value, String fallback, int maxLength) {
        String resolved = StringUtils.hasText(value) ? value.trim() : fallback;
        if (resolved.length() > maxLength) {
            return resolved.substring(0, maxLength);
        }
        return resolved;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private OAuth2AuthenticationException authException(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
