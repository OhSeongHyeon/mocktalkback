package com.mocktalkback.domain.user.entity;

import com.mocktalkback.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 첫 소셜 로그인시 자동 회원가입함, 사용자가 마이페이지에 값 이상하다 느끼면 지 알아서 수정하라하셈 ㅇㅇ
 tb_users 테이블에 값 넣을때
 loginId = handle 로 생성된값 + 랜덤넘버 4자리
 email = 제공자가 준값을 사용하되 null or 공백이면 아래 서술한 방법으로
  - google: email
  - github: email, loginId 값 재활용(handle 로 생성된값 + 랜덤넘버 6자리) + @unknown.unknown
 userName = 제공자가 준값을 사용하되 null or 공백이면 아래 서술한 방법으로
  - google: name 값 사용하지 말고 - google_user
  - github: name 값 사용하지 말고 - github_user
 displayName = 제공자가 준값을 사용하되 null or 공백이면 아래 서술한 방법으로
  - google: name
  - github: name, loginId 값 재활용(handle 로 생성된값 + 랜덤넘버 6자리)
 handle: 값 재활용 없이 원래로직대로 핸들생성함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_user_oauth_links",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_user_oauth_links_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        ),
        @UniqueConstraint(
            name = "uq_tb_user_oauth_links_user_id_provider",
            columnNames = {"user_id", "provider"}
        )
    }
)
public class UserOAuthLinkEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_oauth_link_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "provider", nullable = false, length = 24)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 128)
    private String providerId; // ex) Google "sub"

    @Column(name = "email", length = 128)
    private String email;  // provider가 준 이메일(표시/로그 용)

    private UserOAuthLinkEntity(
        UserEntity user,
        String provider,
        String providerId,
        String email
    ) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
    }

    public static UserOAuthLinkEntity link(
        UserEntity user,
        String provider,
        String providerId,
        String email
    ) {
        if (user == null) throw new IllegalArgumentException("user is required");
        if (provider == null) throw new IllegalArgumentException("provider is required");
        if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId is required");

        return new UserOAuthLinkEntity(user, provider, providerId, email);
    }


}
