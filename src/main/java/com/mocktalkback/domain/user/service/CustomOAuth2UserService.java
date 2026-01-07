package com.mocktalkback.domain.user.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Google은 보통 email/name/sub 제공
        String email = (String) oAuth2User.getAttributes().get("email");
        String name  = (String) oAuth2User.getAttributes().getOrDefault("name", "google-user");
        String sub   = (String) oAuth2User.getAttributes().get("sub"); // google user id

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google email is missing");
        }

        // 우리 시스템 유저 upsert: (email 기준) 없으면 생성, 있으면 그대로
        Long userId = authService.upsertOAuthUser(email, name, sub);

        // successHandler에서 userId 꺼내 쓰기 쉽게 principal에 담아두기
        var attrs = new HashMap<>(oAuth2User.getAttributes());
        attrs.put("mocktalk_user_id", userId);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "email"
        );
    }
}

