package com.mocktalkback.domain.role.entity;

import com.mocktalkback.domain.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    name = "tb_role",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_role_role_name", columnNames = "role_name")
    }
)
public class RoleEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "role_name", nullable = false, length = 24)
    private String roleName;  // 권한명

    @Column(name = "auth_bit", nullable = false)
    private int authBit; // bitmask
    
    @Column(name = "description", nullable = true, length = 36)
    private String description;  // 권한설명

    // ---- 도메인 메서드 ----

    public boolean hasAuth(int mask) {
        return (this.authBit & mask) == mask;
    }

    public void grant(int mask) {
        this.authBit |= mask;
    }

    public void revoke(int mask) {
        this.authBit &= ~mask;
    }

    public void changeAuthBit(int authBit) {
        this.authBit = authBit;
    }

    public static RoleEntity create(String roleName, int authBit, String description) {
        return RoleEntity.builder()
            .roleName(roleName)
            .authBit(authBit)
            .description(description)
            .build();
    }
}
