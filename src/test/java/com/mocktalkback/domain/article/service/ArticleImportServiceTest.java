package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.dto.ArticleImportExecuteResponse;
import com.mocktalkback.domain.article.service.ArticleImportBundleParser.ArticleImportBundle;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class ArticleImportServiceTest {

    @Mock
    private ArticleImportBundleParser articleImportBundleParser;

    @Mock
    private ArticleService articleService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardAccessPolicy boardAccessPolicy;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ArticleImportService articleImportService;

    // 대량 임포트 실행 시 현재 사용자는 role까지 함께 조회해야 한다.
    @Test
    void execute_loads_current_user_with_role() {
        // Given: 비어 있는 번들과 현재 사용자
        MockMultipartFile file = new MockMultipartFile("file", "batch.zip", "application/zip", new byte[] {1, 2, 3});
        UserEntity actor = createUser(1L, RoleNames.MANAGER);
        when(articleImportBundleParser.parse(file)).thenReturn(new ArticleImportBundle("batch.zip", List.of()));
        when(currentUserService.getUserId()).thenReturn(1L);
        when(userRepository.findByIdWithRoleAndDeletedAtIsNull(1L)).thenReturn(Optional.of(actor));

        // When: 대량 임포트를 실행하면
        ArticleImportExecuteResponse response = articleImportService.execute(file);

        // Then: role 포함 사용자 조회를 사용하고, 생성 시도 없이 빈 결과를 반환해야 한다.
        assertThat(response.totalCount()).isZero();
        assertThat(response.successCount()).isZero();
        assertThat(response.failedCount()).isZero();
        verify(userRepository).findByIdWithRoleAndDeletedAtIsNull(1L);
        verify(articleService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    private UserEntity createUser(Long id, String roleName) {
        RoleEntity role = RoleEntity.create(roleName, 0, "테스트 역할");
        UserEntity user = UserEntity.createLocal(
            role,
            "login",
            "user@test.com",
            "pw",
            "user",
            "display",
            "handle"
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
