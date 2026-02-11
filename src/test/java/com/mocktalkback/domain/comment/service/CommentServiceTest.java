package com.mocktalkback.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.article.service.ArticleSyncVersionService;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.dto.CommentReactionSummaryResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionToggleRequest;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentReactionRepository;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.moderation.repository.SanctionRepository;
import com.mocktalkback.domain.notification.service.NotificationService;
import com.mocktalkback.domain.realtime.service.BoardRealtimeSseService;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SanctionRepository sanctionRepository;

    @Mock
    private BoardRealtimeSseService boardRealtimeSseService;

    @Mock
    private ArticleSyncVersionService articleSyncVersionService;

    @InjectMocks
    private CommentService commentService;

    // 댓글 반응 토글은 원자 upsert를 사용해 경합 상황에서도 일관된 집계를 반환해야 한다.
    @Test
    void toggleReaction_uses_atomic_upsert_and_returns_summary() {
        // Given: 공개 게시글의 댓글 반응 토글 요청
        BoardEntity board = createBoard(1L);
        UserEntity user = createUser(2L);
        ArticleEntity article = createArticle(10L, board, user);
        CommentEntity comment = createComment(100L, article, user);

        when(currentUserService.getUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(articleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(article));
        when(boardMemberRepository.findByUserIdAndBoardId(2L, 1L)).thenReturn(Optional.empty());
        when(sanctionRepository.existsActiveSanction(anyLong(), any(), any(), anyLong(), any()))
            .thenReturn(false);
        when(commentReactionRepository.upsertToggleReaction(2L, 100L, (short) 1)).thenReturn((short) 1);
        when(commentReactionRepository.countByCommentIdAndReactionType(100L, (short) 1)).thenReturn(3L);
        when(commentReactionRepository.countByCommentIdAndReactionType(100L, (short) -1)).thenReturn(1L);

        // When: 반응 토글 실행
        CommentReactionSummaryResponse response = commentService.toggleReaction(
            100L,
            new CommentReactionToggleRequest((short) 1)
        );

        // Then: 원자 토글 결과와 집계가 반환되어야 한다.
        assertThat(response.commentId()).isEqualTo(100L);
        assertThat(response.myReaction()).isEqualTo((short) 1);
        assertThat(response.likeCount()).isEqualTo(3L);
        assertThat(response.dislikeCount()).isEqualTo(1L);
        verify(commentReactionRepository).upsertToggleReaction(2L, 100L, (short) 1);
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

    private ArticleEntity createArticle(Long id, BoardEntity board, UserEntity user) {
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(null)
            .visibility(ContentVisibility.PUBLIC)
            .title("title")
            .content("content")
            .notice(false)
            .hit(0)
            .build();
        ReflectionTestUtils.setField(article, "id", id);
        return article;
    }

    private CommentEntity createComment(Long id, ArticleEntity article, UserEntity user) {
        CommentEntity comment = CommentEntity.builder()
            .user(user)
            .article(article)
            .parentComment(null)
            .rootComment(null)
            .depth(0)
            .content("comment")
            .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
