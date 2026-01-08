package com.mocktalkback.domain.user.entity;

import com.mocktalkback.domain.common.entity.SoftDeleteEntity;
import com.mocktalkback.domain.role.entity.RoleEntity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_users_login_id", columnNames = "login_id"),
        @UniqueConstraint(name = "uq_tb_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_tb_users_handle", columnNames = "handle")
    },
    indexes = {
        @Index(name = "ix_tb_users_role_id", columnList = "role_id"),
    }
)
public class UserEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "role_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_users_tb_role")
    )
    private RoleEntity role;

    @Column(name = "login_id", nullable = false, length = 128)
    private String loginId;

    @Column(name = "email", nullable = false, length = 128)
    private String email;

    @Column(name = "pw_hash", nullable = false, length = 255)
    private String pwHash;

    @Column(name = "user_name", nullable = false, length = 32)
    private String userName;

    @Column(name = "display_name", nullable = false, length = 16)
    private String displayName;

    @Column(name = "handle", nullable = false, length = 24)
    private String handle;

    @Column(name = "user_point", nullable = false)
    private int userPoint;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Builder(access = AccessLevel.PRIVATE)
    private UserEntity(
            RoleEntity role,
            String loginId,
            String email,
            String pwHash,
            String userName,
            String displayName,
            String handle,
            int userPoint,
            boolean enabled,
            boolean locked
    ) {
        this.role = role;
        this.loginId = loginId;
        this.email = email;
        this.pwHash = pwHash;
        this.userName = userName;
        this.displayName = displayName;
        this.handle = handle;
        this.userPoint = userPoint;
        this.enabled = enabled;
        this.locked = locked;
    }

    public static UserEntity createLocal(
            RoleEntity role,
            String loginId,
            String email,
            String encodedPw,
            String userName,
            String displayName,
            String handle
    ) {
        return UserEntity.builder()
                .role(role)
                .loginId(loginId)
                .email(email)
                .pwHash(encodedPw)
                .userName(userName)
                .displayName(displayName)
                .handle(handle)
                .userPoint(0)
                .enabled(true)
                .locked(false)
                .build();
    }

    public void changePassword(String encodedPw) {
        this.pwHash = encodedPw;
    }

    public void changePoint(int delta) {
        this.userPoint += delta;
    }

}
