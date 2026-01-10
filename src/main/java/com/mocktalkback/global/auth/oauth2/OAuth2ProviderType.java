package com.mocktalkback.global.auth.oauth2;

// (OIDC) ID Token claims: sub/iss 같은 표준 클레임이 존재 (예: Google)
// (OAuth2-only) GitHub처럼 OIDC가 아닌 제공자는 sub/iss가 없고, /user 응답의 id 등을 사용

// provider    // 제공자 식별자 (google, github 등)

// providerId  // 제공자 내부 고유 사용자 ID
               // - OIDC: sub (Subject)
               // - OAuth2-only: userInfo의 id (예: GitHub /user.id)

// issuer     // OIDC Issuer (iss). OIDC 제공자에서만 의미 있음.
              // OAuth2-only 제공자는 null/빈값 가능

// email      // 이메일 (제공자 정책/권한(scope)에 따라 null 가능)
              // GitHub는 user:email scope + /user/emails 추가 호출 필요할 수 있음

// name       // 사용자 표시 이름(프로필 이름). 설정에 따라 null/변경 가능
public class OAuth2ProviderType {
    private OAuth2ProviderType() {}
    public static final String GOOGLE = "google";
    public static final String GITHUB = "github";

    public static boolean isSupported(String provider) {
        return GOOGLE.equals(provider) || GITHUB.equals(provider);
    }

}
