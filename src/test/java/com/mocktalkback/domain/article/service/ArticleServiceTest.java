package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleReactionSummaryResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionToggleRequest;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleFileEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleBookmarkRepository;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.article.repository.ArticleFileRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.TemporaryFilePolicy;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.domain.moderation.repository.SanctionRepository;
import com.mocktalkback.domain.realtime.service.BoardRealtimeSseService;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.sanitize.HtmlSanitizer;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BoardFileRepository boardFileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleCategoryRepository articleCategoryRepository;

    @Mock
    private ArticleFileRepository articleFileRepository;

    @Mock
    private ArticleBookmarkRepository articleBookmarkRepository;

    @Mock
    private ArticleReactionRepository articleReactionRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private TemporaryFilePolicy temporaryFilePolicy;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    @Mock
    private SanctionRepository sanctionRepository;

    @Mock
    private BoardRealtimeSseService boardRealtimeSseService;

    @InjectMocks
    private ArticleService articleService;

    // 게시글 생성 시 fileIds가 매핑되고 임시 상태가 해제되어야 한다.
    @Test
    void create_attaches_files_and_clears_temporary() {
        // Given: 게시글 생성 요청과 파일 목록
        BoardEntity board = createBoard(1L);
        UserEntity user = createUser(2L);
        ArticleCategoryEntity category = createCategory(3L, board);
        ArticleCreateRequest request = new ArticleCreateRequest(
            1L,
            2L,
            3L,
            ContentVisibility.PUBLIC,
            "title",
            "content",
            false,
            List.of(10L, 20L)
        );

        when(boardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(board));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(articleCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(sanctionRepository.existsActiveSanction(anyLong(), any(), any(), anyLong(), any()))
            .thenReturn(false);
        when(htmlSanitizer.sanitize("content")).thenReturn("content");

        ArticleEntity article = createArticle(100L, board, user, category);
        when(articleMapper.toEntity(any(ArticleCreateRequest.class), eq(board), eq(user), eq(category)))
            .thenReturn(article);
        when(articleRepository.save(article)).thenReturn(article);

        FileClassEntity fileClass = createFileClass(FileClassCode.ARTICLE_CONTENT_IMAGE);
        FileEntity fileA = createFile(10L, fileClass, Instant.parse("2024-01-01T00:00:00Z"));
        FileEntity fileB = createFile(20L, fileClass, Instant.parse("2024-01-01T00:00:00Z"));
        when(fileRepository.findById(10L)).thenReturn(Optional.of(fileA));
        when(fileRepository.findById(20L)).thenReturn(Optional.of(fileB));

        // When: 게시글 생성 호출
        articleService.create(request);

        // Then: 매핑 저장과 임시 해제 확인
        verify(articleFileRepository, times(2)).save(any(ArticleFileEntity.class));
        assertThat(fileA.getTempExpiresAt()).isNull();
        assertThat(fileB.getTempExpiresAt()).isNull();
    }

    // 게시글 수정 시 신규 파일은 매핑하고 제거된 파일은 임시 처리해야 한다.
    @Test
    void update_syncs_files_and_marks_removed_as_temporary() {
        // Given: 기존 게시글과 첨부 상태
        BoardEntity board = createBoard(1L);
        UserEntity user = createUser(2L);
        ArticleCategoryEntity category = createCategory(3L, board);
        ArticleEntity article = createArticle(100L, board, user, category);

        when(articleRepository.findById(100L)).thenReturn(Optional.of(article));
        when(currentUserService.getUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(articleCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(sanctionRepository.existsActiveSanction(anyLong(), any(), any(), anyLong(), any()))
            .thenReturn(false);
        when(htmlSanitizer.sanitize("content")).thenReturn("content");

        FileClassEntity fileClass = createFileClass(FileClassCode.ARTICLE_CONTENT_IMAGE);
        FileEntity existingFile = createFile(10L, fileClass, null);
        ArticleFileEntity existingMapping = ArticleFileEntity.builder()
            .article(article)
            .file(existingFile)
            .build();
        when(articleFileRepository.findAllByArticleIdOrderByCreatedAtAsc(100L))
            .thenReturn(List.of(existingMapping));
        when(articleFileRepository.existsByFileId(10L)).thenReturn(false);

        FileEntity newFile = createFile(20L, fileClass, Instant.parse("2024-01-01T00:00:00Z"));
        when(fileRepository.findById(20L)).thenReturn(Optional.of(newFile));

        Instant expiry = Instant.parse("2024-02-01T00:00:00Z");
        when(temporaryFilePolicy.resolveExpiry()).thenReturn(expiry);

        ArticleUpdateRequest request = new ArticleUpdateRequest(
            3L,
            ContentVisibility.PUBLIC,
            "title",
            "content",
            false,
            List.of(20L)
        );

        // When: 게시글 수정 호출
        articleService.update(100L, request);

        // Then: 신규 매핑과 제거된 파일 임시 처리 확인
        verify(articleFileRepository).save(any(ArticleFileEntity.class));
        verify(articleFileRepository).delete(existingMapping);
        assertThat(existingFile.getTempExpiresAt()).isEqualTo(expiry);
        assertThat(newFile.getTempExpiresAt()).isNull();
    }

    // 반응 토글은 원자 upsert를 사용해 경합 상황에서도 일관된 결과를 반환해야 한다.
    @Test
    void toggleReaction_uses_atomic_upsert_and_returns_summary() {
        // Given: 공개 게시글에 대한 반응 토글 요청
        BoardEntity board = createBoard(1L);
        UserEntity user = createUser(2L);
        ArticleEntity article = createArticle(10L, board, user, null);

        when(currentUserService.getUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(articleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(article));
        when(boardMemberRepository.findByUserIdAndBoardId(2L, 1L)).thenReturn(Optional.empty());
        when(sanctionRepository.existsActiveSanction(anyLong(), any(), any(), anyLong(), any()))
            .thenReturn(false);
        when(articleReactionRepository.upsertToggleReaction(2L, 10L, (short) 1)).thenReturn((short) 1);
        when(articleReactionRepository.countByArticleIdAndReactionType(10L, (short) 1)).thenReturn(5L);
        when(articleReactionRepository.countByArticleIdAndReactionType(10L, (short) -1)).thenReturn(2L);

        // When: 반응 토글 실행
        ArticleReactionSummaryResponse response = articleService.toggleReaction(
            10L,
            new ArticleReactionToggleRequest((short) 1)
        );

        // Then: 원자 토글 결과와 집계가 반환되어야 한다.
        assertThat(response.articleId()).isEqualTo(10L);
        assertThat(response.myReaction()).isEqualTo((short) 1);
        assertThat(response.likeCount()).isEqualTo(5L);
        assertThat(response.dislikeCount()).isEqualTo(2L);
        verify(articleReactionRepository).upsertToggleReaction(2L, 10L, (short) 1);
    }

    private BoardEntity createBoard(Long id) {
        BoardEntity board = BoardEntity.builder()
            .boardName("notice")
            .slug("notice")
            .description("테스트 게시판")
            .visibility(BoardVisibility.PUBLIC)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private UserEntity createUser(Long id) {
        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
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

    private ArticleCategoryEntity createCategory(Long id, BoardEntity board) {
        ArticleCategoryEntity category = ArticleCategoryEntity.builder()
            .board(board)
            .categoryName("공지")
            .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private ArticleEntity createArticle(
        Long id,
        BoardEntity board,
        UserEntity user,
        ArticleCategoryEntity category
    ) {
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(category)
            .visibility(ContentVisibility.PUBLIC)
            .title("title")
            .content("content")
            .notice(false)
            .hit(0)
            .build();
        ReflectionTestUtils.setField(article, "id", id);
        return article;
    }

    private FileClassEntity createFileClass(String code) {
        return FileClassEntity.builder()
            .code(code)
            .name("테스트 파일 클래스")
            .description("테스트")
            .mediaKind(MediaKind.IMAGE)
            .build();
    }

    private FileEntity createFile(Long id, FileClassEntity fileClass, Instant tempExpiresAt) {
        FileEntity file = FileEntity.builder()
            .fileClass(fileClass)
            .fileName("file.png")
            .storageKey("/uploads/test/file.png")
            .fileSize(123L)
            .mimeType("image/png")
            .metadataPreserved(false)
            .tempExpiresAt(tempExpiresAt)
            .build();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
