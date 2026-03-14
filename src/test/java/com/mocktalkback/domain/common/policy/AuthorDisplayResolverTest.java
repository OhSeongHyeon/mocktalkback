package com.mocktalkback.domain.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

class AuthorDisplayResolverTest {

    private final AuthorDisplayResolver authorDisplayResolver = new AuthorDisplayResolver();

    // displayName이 있으면 작성자 표시는 displayName을 반환해야 한다.
    @Test
    void resolve_author_name_should_return_display_name_when_present() {
        // Given: displayName이 있는 사용자
        UserEntity user = createUser("display-name", "user-name", "handle");

        // When: 작성자 표시명을 계산
        String result = authorDisplayResolver.resolveAuthorName(user);

        // Then: displayName이 반환되어야 함
        assertThat(result).isEqualTo("display-name");
    }

    // displayName이 비어 있으면 작성자 표시는 userName을 반환해야 한다.
    @Test
    void resolve_author_name_should_fallback_to_user_name() {
        // Given: displayName이 비어 있는 사용자
        UserEntity user = createUser(" ", "user-name", "handle");

        // When: 작성자 표시명을 계산
        String result = authorDisplayResolver.resolveAuthorName(user);

        // Then: userName이 반환되어야 함
        assertThat(result).isEqualTo("user-name");
    }

    // 게시판 소유자 표시는 displayName과 handle을 조합해 반환해야 한다.
    @Test
    void format_owner_display_should_combine_display_name_and_handle() {
        // Given: displayName과 handle이 모두 있는 사용자
        UserEntity user = createUser("display", "user-name", "owner");

        // When: 소유자 표시명을 계산
        String result = authorDisplayResolver.formatOwnerDisplay(user);

        // Then: displayName@handle 형식이어야 함
        assertThat(result).isEqualTo("display@owner");
    }

    private UserEntity createUser(String displayName, String userName, String handle) {
        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
        return UserEntity.createLocal(
            role,
            "login-" + userName,
            userName + "@test.com",
            "pw",
            userName,
            displayName,
            handle
        );
    }
}

