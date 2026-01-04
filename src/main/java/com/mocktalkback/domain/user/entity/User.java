package com.mocktalkback.domain.user.entity;

import com.mocktalkback.domain.common.entity.SoftDeleteEntity;
import com.mocktalkback.global.auth.entity.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
    name = "tb_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_users_email", columnNames = "email"),
        @UniqueConstraint(name = "UQ_users_handle", columnNames = "handle")
    },
    indexes = {
        @Index(name = "IDX_users_role_id", columnList = "role_id")
    }
)
public class User extends SoftDeleteEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "role_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_tb_role_TO_tb_users")
    )
    private Role role;

    // @Column(name = "file_id")
    // private Long fileId;

    // 일단은 이메일형식 검사안하고 로그인(로컬로그인 + jwt) 기능 구현 먼저하고
    @Column(name = "email", nullable = false, length = 128)
    private String email;  // 이메일, ID

    @Column(name = "pw_hash", nullable = false, length = 60)
    private String pwHash;  // bcrypt 해시

    @Column(name = "user_name", nullable = false, length = 32)
    private String userName;  // 실명

    @Column(name = "display_name", nullable = false, length = 16)
    private String displayName;  // 표시 닉네임(중복 허용)

    @Column(name = "handle", nullable = false, length = 24)
    private String handle;  // @핸들(고유)

    @Builder.Default
    @Column(name = "user_point", nullable = false)
    private int userPoint = 0;  // 포인트

    // ---- 팩토리 / 도메인 메서드 ----

    public static User createLocal(
            Role role,
            String email,
            String encodedPw,
            String userName,
            String displayName,
            String handle
    ) {
        return User.builder()
                .role(role)
                .email(email)
                .pwHash(encodedPw)
                .userName(userName)
                .displayName(displayName)
                .handle(handle)
                .userPoint(0)
                .build();
    }

    public void changePassword(String encodedPw) {
        this.pwHash = encodedPw;
    }

    // 마이너스 포인트 가능
    public void addPoint(int amount) {
        this.userPoint += amount;
    }

    public void subtractPoint(int amount) {
        this.userPoint -= amount;
    }

}
