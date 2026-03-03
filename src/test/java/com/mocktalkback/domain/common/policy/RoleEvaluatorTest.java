package com.mocktalkback.domain.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

class RoleEvaluatorTest {

    private final RoleEvaluator roleEvaluator = new RoleEvaluator();

    // ADMIN 역할 사용자는 관리자 권한으로 판단해야 한다.
    @Test
    void is_manager_or_admin_should_return_true_for_admin_role() {
        // Given: ADMIN 역할 사용자
        UserEntity admin = createUser("ADMIN");

        // When: 관리자 여부를 판별
        boolean result = roleEvaluator.isManagerOrAdmin(admin);

        // Then: true여야 함
        assertThat(result).isTrue();
    }

    // USER 역할 사용자는 관리자 권한으로 판단하지 않아야 한다.
    @Test
    void is_manager_or_admin_should_return_false_for_user_role() {
        // Given: USER 역할 사용자
        UserEntity user = createUser("USER");

        // When: 관리자 여부를 판별
        boolean result = roleEvaluator.isManagerOrAdmin(user);

        // Then: false여야 함
        assertThat(result).isFalse();
    }

    // ADMIN 역할이 아니면 관리자 전용 권한으로 판단하지 않아야 한다.
    @Test
    void is_admin_should_return_false_for_manager_role() {
        // Given: MANAGER 역할 사용자
        UserEntity manager = createUser("MANAGER");

        // When: 관리자 전용 여부를 판별
        boolean result = roleEvaluator.isAdmin(manager);

        // Then: false여야 함
        assertThat(result).isFalse();
    }

    private UserEntity createUser(String roleName) {
        RoleEntity role = RoleEntity.create(roleName, 0, "테스트");
        return UserEntity.createLocal(
            role,
            "login-" + roleName,
            roleName.toLowerCase() + "@test.com",
            "pw",
            "user-" + roleName,
            "display-" + roleName,
            "handle-" + roleName
        );
    }
}
