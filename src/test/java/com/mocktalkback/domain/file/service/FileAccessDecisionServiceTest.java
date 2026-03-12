package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleFileEntity;
import com.mocktalkback.domain.article.repository.ArticleFileRepository;
import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class FileAccessDecisionServiceTest {

    @Mock
    private ArticleFileRepository articleFileRepository;

    @Mock
    private BoardFileRepository boardFileRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private BoardAccessPolicy boardAccessPolicy;

    // 임시 파일은 업로더 본인에게만 보호 조회로 허용되어야 한다.
    @Test
    void decide_allows_temporary_article_file_to_owner_only() {
        // given: 임시 본문 이미지 파일과 업로더 본인 인증 정보가 있다.
        FileAccessDecisionService service = new FileAccessDecisionService(
            articleFileRepository,
            boardFileRepository,
            boardMemberRepository,
            userRepository,
            currentUserService,
            boardAccessPolicy,
            "uploads"
        );
        FileEntity file = createFileEntity(10L, FileClassCode.ARTICLE_CONTENT_IMAGE, "uploads/article_content_image/7/2026/03/12/file.png");
        file.markTemporary(Instant.now().plusSeconds(600));
        when(currentUserService.getOptionalUserId()).thenReturn(Optional.of(7L));
        when(userRepository.findByIdWithRoleAndDeletedAtIsNull(7L)).thenReturn(Optional.of(createUserEntity(7L)));

        // when: 파일 접근 판정을 수행하면
        FileAccessDecision decision = service.decide(file);

        // then: 본인에게만 보호 조회를 허용한다.
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.deliveryMode()).isEqualTo(FileDeliveryMode.PROTECTED);
    }

    // 비공개 게시글 본문 파일은 게시글 접근이 불가능하면 조회를 허용하면 안 된다.
    @Test
    void decide_denies_article_file_when_article_visibility_is_not_allowed() {
        // given: 비공개 게시글에 연결된 본문 파일이 있고, 현재 사용자는 게시글 가시성 허용 범위에 없다.
        FileAccessDecisionService service = new FileAccessDecisionService(
            articleFileRepository,
            boardFileRepository,
            boardMemberRepository,
            userRepository,
            currentUserService,
            boardAccessPolicy,
            "uploads"
        );
        FileEntity file = createFileEntity(11L, FileClassCode.ARTICLE_CONTENT_IMAGE, "uploads/article_content_image/9/2026/03/12/file.png");
        ArticleEntity article = createArticleEntity(100L, BoardVisibility.PUBLIC, ContentVisibility.MEMBERS);
        ArticleFileEntity mapping = ArticleFileEntity.builder()
            .article(article)
            .file(file)
            .build();

        when(currentUserService.getOptionalUserId()).thenReturn(Optional.empty());
        when(articleFileRepository.findAllByFileId(11L)).thenReturn(List.of(mapping));
        when(boardAccessPolicy.canAccessBoard(article.getBoard(), null, null)).thenReturn(true);
        when(boardAccessPolicy.resolveAllowedVisibilities(article.getBoard(), null, null))
            .thenReturn(EnumSet.of(ContentVisibility.PUBLIC));

        // when: 파일 접근 판정을 수행하면
        FileAccessDecision decision = service.decide(file);

        // then: 게시글 가시성이 허용되지 않으므로 조회를 거부한다.
        assertThat(decision.allowed()).isFalse();
    }

    // 공개 게시판 대표 이미지는 공개 전달 모드로 허용되어야 한다.
    @Test
    void decide_returns_public_delivery_for_public_board_image() {
        // given: 공개 게시판에 연결된 게시판 대표 이미지가 있다.
        FileAccessDecisionService service = new FileAccessDecisionService(
            articleFileRepository,
            boardFileRepository,
            boardMemberRepository,
            userRepository,
            currentUserService,
            boardAccessPolicy,
            "uploads"
        );
        FileEntity file = createFileEntity(12L, FileClassCode.BOARD_IMAGE, "uploads/board_image/5/2026/03/12/board.png");
        BoardEntity board = createBoardEntity(33L, BoardVisibility.PUBLIC);
        BoardFileEntity mapping = BoardFileEntity.builder()
            .board(board)
            .file(file)
            .build();

        when(currentUserService.getOptionalUserId()).thenReturn(Optional.empty());
        when(boardFileRepository.findAllByFileId(12L)).thenReturn(List.of(mapping));
        when(boardAccessPolicy.canAccessBoard(board, null, null)).thenReturn(true);

        // when: 파일 접근 판정을 수행하면
        FileAccessDecision decision = service.decide(file);

        // then: 공개 전달 모드로 허용한다.
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.deliveryMode()).isEqualTo(FileDeliveryMode.PUBLIC);
    }

    private FileEntity createFileEntity(Long id, String fileClassCode, String storageKey) {
        FileClassEntity fileClassEntity = FileClassEntity.builder()
            .code(fileClassCode)
            .name("테스트 파일")
            .description("테스트")
            .mediaKind(MediaKind.IMAGE)
            .build();

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClassEntity)
            .fileName("file.png")
            .storageKey(storageKey)
            .fileSize(2048L)
            .mimeType("image/png")
            .metadataPreserved(false)
            .build();
        ReflectionTestUtils.setField(fileEntity, "id", id);
        return fileEntity;
    }

    private ArticleEntity createArticleEntity(Long boardId, BoardVisibility boardVisibility, ContentVisibility visibility) {
        BoardEntity board = createBoardEntity(boardId, boardVisibility);
        UserEntity user = createUserEntity(3L);
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .visibility(visibility)
            .title("테스트")
            .content("본문")
            .contentSource("본문")
            .contentFormat(ArticleContentFormat.HTML)
            .notice(false)
            .build();
        ReflectionTestUtils.setField(article, "id", 99L);
        return article;
    }

    private BoardEntity createBoardEntity(Long id, BoardVisibility visibility) {
        BoardEntity board = BoardEntity.builder()
            .boardName("테스트 게시판")
            .slug("test-board")
            .description("설명")
            .visibility(visibility)
            .articleWritePolicy(BoardArticleWritePolicy.ALL_AUTHENTICATED)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private UserEntity createUserEntity(Long id) {
        RoleEntity role = RoleEntity.create("ROLE_USER", 0, "일반 사용자");
        UserEntity user = UserEntity.createLocal(
            role,
            "tester",
            "tester@example.com",
            "pw",
            "tester",
            "테스터",
            "tester"
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
