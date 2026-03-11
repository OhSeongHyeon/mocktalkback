package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleImportExecuteResponse;
import com.mocktalkback.domain.article.dto.ArticleImportPreviewResponse;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.article.service.ArticleImportBundleParser.ArticleImportBundle;
import com.mocktalkback.domain.article.service.ArticleImportBundleParser.ArticleImportCandidate;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.role.type.ContentVisibility;
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
    private ArticleCategoryRepository articleCategoryRepository;

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
        ArticleImportExecuteResponse response = articleImportService.execute(file, true);

        // Then: role 포함 사용자 조회를 사용하고, 생성 시도 없이 빈 결과를 반환해야 한다.
        assertThat(response.totalCount()).isZero();
        assertThat(response.successCount()).isZero();
        assertThat(response.failedCount()).isZero();
        verify(userRepository).findByIdWithRoleAndDeletedAtIsNull(1L);
        verify(articleService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    // 대량 임포트는 categoryName을 게시판 카테고리 ID로 변환해 생성 요청에 전달해야 한다.
    @Test
    void preview_marks_missing_category_as_warning_when_auto_create_enabled() {
        // Given: 없는 카테고리를 자동 생성하도록 한 미리보기 요청
        MockMultipartFile file = new MockMultipartFile("file", "batch.zip", "application/zip", new byte[] {1, 2, 3});
        UserEntity actor = createUser(1L, RoleNames.MANAGER);
        BoardEntity board = createBoard(10L, "dev");
        ArticleImportCandidate candidate = new ArticleImportCandidate(
            "posts/post-1.md",
            "카테고리 테스트",
            "dev",
            "PUBLIC",
            "없는 카테고리",
            "# 본문",
            List.of(),
            List.of()
        );

        when(articleImportBundleParser.parse(file)).thenReturn(new ArticleImportBundle("batch.zip", List.of(candidate)));
        when(currentUserService.getUserId()).thenReturn(1L);
        when(userRepository.findByIdWithRoleAndDeletedAtIsNull(1L)).thenReturn(Optional.of(actor));
        when(boardRepository.findBySlugAndDeletedAtIsNull("dev")).thenReturn(Optional.of(board));
        when(boardMemberRepository.findByUserIdAndBoardId(1L, 10L)).thenReturn(Optional.empty());
        when(boardAccessPolicy.canAccessBoard(board, actor, null)).thenReturn(true);
        when(boardAccessPolicy.resolveAllowedVisibilities(board, actor, null)).thenReturn(java.util.EnumSet.of(ContentVisibility.PUBLIC));
        when(articleCategoryRepository.findByBoardIdAndCategoryNameIgnoreCase(10L, "없는 카테고리")).thenReturn(Optional.empty());

        // When: 미리보기를 실행하면
        ArticleImportPreviewResponse response = articleImportService.preview(file, true);

        // Then: 오류 대신 자동 생성 경고와 함께 실행 가능 상태여야 한다.
        assertThat(response.canExecute()).isTrue();
        assertThat(response.executableCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.executable()).isTrue();
            assertThat(item.errors()).isEmpty();
            assertThat(item.warnings()).contains("게시판 카테고리가 없어 실행 시 자동 생성합니다: 없는 카테고리");
        });
    }

    // frontmatter만 있고 본문이 없는 Markdown은 실행 불가로 판단해야 한다.
    @Test
    void preview_rejects_frontmatter_only_markdown() {
        // Given: frontmatter만 있고 본문이 없는 대량 임포트 후보
        MockMultipartFile file = new MockMultipartFile("file", "batch.zip", "application/zip", new byte[] {1, 2, 3});
        UserEntity actor = createUser(1L, RoleNames.MANAGER);
        BoardEntity board = createBoard(10L, "dev");
        ArticleImportCandidate candidate = new ArticleImportCandidate(
            "posts/post-1.md",
            "카테고리 테스트",
            "dev",
            "PUBLIC",
            null,
            """
            ---
            title: "카테고리 테스트"
            visibility: "PUBLIC"
            ---
            """,
            List.of(),
            List.of()
        );

        when(articleImportBundleParser.parse(file)).thenReturn(new ArticleImportBundle("batch.zip", List.of(candidate)));
        when(currentUserService.getUserId()).thenReturn(1L);
        when(userRepository.findByIdWithRoleAndDeletedAtIsNull(1L)).thenReturn(Optional.of(actor));
        when(boardRepository.findBySlugAndDeletedAtIsNull("dev")).thenReturn(Optional.of(board));
        when(boardMemberRepository.findByUserIdAndBoardId(1L, 10L)).thenReturn(Optional.empty());
        when(boardAccessPolicy.canAccessBoard(board, actor, null)).thenReturn(true);
        when(boardAccessPolicy.resolveAllowedVisibilities(board, actor, null)).thenReturn(java.util.EnumSet.of(ContentVisibility.PUBLIC));

        // When: 미리보기를 실행하면
        ArticleImportPreviewResponse response = articleImportService.preview(file, true);

        // Then: 본문이 비어 있어 실행할 수 없어야 한다.
        assertThat(response.canExecute()).isFalse();
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.executable()).isFalse();
            assertThat(item.errors()).contains("본문이 비어 있습니다.");
        });
    }

    // 대량 임포트는 autoCreate가 켜져 있으면 없는 categoryName을 먼저 만들고 생성 요청에 전달해야 한다.
    @Test
    void execute_creates_missing_category_when_auto_create_enabled() {
        // Given: 카테고리 이름이 포함된 대량 임포트 후보와 작성 권한이 있는 사용자
        MockMultipartFile file = new MockMultipartFile("file", "batch.zip", "application/zip", new byte[] {1, 2, 3});
        UserEntity actor = createUser(1L, RoleNames.MANAGER);
        BoardEntity board = createBoard(10L, "dev");
        ArticleCategoryEntity category = createCategory(30L, board, "백엔드");
        ArticleImportCandidate candidate = new ArticleImportCandidate(
            "posts/post-1.md",
            "카테고리 테스트",
            "dev",
            "PUBLIC",
            "백엔드",
            "# 본문",
            List.of(),
            List.of()
        );

        when(articleImportBundleParser.parse(file)).thenReturn(new ArticleImportBundle("batch.zip", List.of(candidate)));
        when(currentUserService.getUserId()).thenReturn(1L);
        when(userRepository.findByIdWithRoleAndDeletedAtIsNull(1L)).thenReturn(Optional.of(actor));
        when(boardRepository.findBySlugAndDeletedAtIsNull("dev")).thenReturn(Optional.of(board));
        when(boardMemberRepository.findByUserIdAndBoardId(1L, 10L)).thenReturn(Optional.empty());
        when(boardAccessPolicy.canAccessBoard(board, actor, null)).thenReturn(true);
        when(boardAccessPolicy.resolveAllowedVisibilities(board, actor, null)).thenReturn(java.util.EnumSet.of(ContentVisibility.PUBLIC));
        when(articleCategoryRepository.findByBoardIdAndCategoryNameIgnoreCase(10L, "백엔드")).thenReturn(Optional.empty());
        when(boardRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(board));
        when(articleCategoryRepository.save(argThat(saved -> isExpectedCategory(saved, board, "백엔드")))).thenReturn(category);
        when(articleService.create(argThat(request -> isExpectedCreateRequest(request, board.getId(), actor.getId(), category.getId()))))
            .thenReturn(new ArticleResponse(
                99L,
                board.getId(),
                actor.getId(),
                category.getId(),
                ContentVisibility.PUBLIC,
                "카테고리 테스트",
                "<p>본문</p>",
                0L,
                false,
                null,
                null,
                null
            ));

        // When: 대량 임포트를 실행하면
        ArticleImportExecuteResponse response = articleImportService.execute(file, true);

        // Then: 카테고리 ID가 포함된 생성 요청으로 게시글이 만들어져야 한다.
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.created()).isTrue();
            assertThat(item.categoryName()).isEqualTo("백엔드");
            assertThat(item.articleId()).isEqualTo(99L);
        });
        verify(articleCategoryRepository).save(argThat(saved -> isExpectedCategory(saved, board, "백엔드")));
        verify(articleService).create(argThat(request -> isExpectedCreateRequest(request, board.getId(), actor.getId(), category.getId())));
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

    private BoardEntity createBoard(Long id, String slug) {
        BoardEntity board = BoardEntity.builder()
            .boardName("테스트 게시판")
            .slug(slug)
            .description("설명")
            .visibility(BoardVisibility.PUBLIC)
            .articleWritePolicy(BoardArticleWritePolicy.ALL_AUTHENTICATED)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private ArticleCategoryEntity createCategory(Long id, BoardEntity board, String categoryName) {
        ArticleCategoryEntity category = ArticleCategoryEntity.builder()
            .board(board)
            .categoryName(categoryName)
            .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private boolean isExpectedCreateRequest(ArticleCreateRequest request, Long boardId, Long userId, Long categoryId) {
        return request != null
            && boardId.equals(request.boardId())
            && userId.equals(request.userId())
            && categoryId.equals(request.categoryId())
            && request.visibility() == ContentVisibility.PUBLIC
            && "카테고리 테스트".equals(request.title())
            && "# 본문".equals(request.contentSource());
    }

    private boolean isExpectedCategory(ArticleCategoryEntity category, BoardEntity board, String categoryName) {
        return category != null
            && category.getBoard() == board
            && categoryName.equals(category.getCategoryName());
    }
}
