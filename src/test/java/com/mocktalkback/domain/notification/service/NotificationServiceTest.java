package com.mocktalkback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.common.policy.PageNormalizer;
import com.mocktalkback.domain.notification.entity.NotificationEntity;
import com.mocktalkback.domain.notification.repository.NotificationRepository;
import com.mocktalkback.domain.realtime.service.NotificationPresenceService;
import com.mocktalkback.domain.realtime.service.NotificationRealtimeSseService;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationPresenceService notificationPresenceService;

    @Mock
    private NotificationRealtimeSseService notificationRealtimeSseService;

    @Spy
    private PageNormalizer pageNormalizer = new PageNormalizer();

    @Spy
    private AuthorDisplayResolver authorDisplayResolver = new AuthorDisplayResolver();

    @InjectMocks
    private NotificationService notificationService;

    // 댓글 대댓글 알림 대상이 동일 게시글 상세를 보고 있으면 읽음 상태로 저장해야 한다.
    @Test
    void create_comment_reply_should_save_as_read_when_receiver_viewing_same_article_detail() {
        // Given: 수신자가 동일 게시글 상세 화면을 보고 있는 상태
        UserEntity receiver = createUser(10L, "receiver");
        UserEntity sender = createUser(20L, "sender");
        BoardEntity board = createBoard(1L, "team-board");
        ArticleEntity article = createArticle(30L, board, receiver);
        CommentEntity reply = createComment(40L, article, sender);

        when(notificationPresenceService.isViewingArticleDetail(10L, 30L)).thenReturn(true);

        // When: 대댓글 알림 생성 요청
        notificationService.createCommentReply(receiver, sender, article, reply);

        // Then: 알림은 읽음 상태로 저장되고 unread 카운트 푸시는 발생하지 않아야 함
        ArgumentCaptor<NotificationEntity> entityCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, times(1)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().isRead()).isTrue();
        verify(notificationRepository, never()).countByUserIdAndReadFalse(anyLong());
        verify(notificationRealtimeSseService, never()).publishUnreadCountChanged(anyLong(), anyLong());
    }

    // 게시글 댓글 알림 대상이 동일 게시글 상세를 보고 있으면 읽음 상태로 저장해야 한다.
    @Test
    void create_article_comment_should_save_as_read_when_receiver_viewing_same_article_detail() {
        // Given: 수신자가 동일 게시글 상세 화면을 보고 있는 상태
        UserEntity receiver = createUser(11L, "receiver2");
        UserEntity sender = createUser(21L, "sender2");
        BoardEntity board = createBoard(2L, "notice");
        ArticleEntity article = createArticle(31L, board, receiver);

        when(notificationPresenceService.isViewingArticleDetail(11L, 31L)).thenReturn(true);

        // When: 댓글 알림 생성 요청
        notificationService.createArticleComment(receiver, sender, article);

        // Then: 알림은 읽음 상태로 저장되고 unread 카운트 푸시는 발생하지 않아야 함
        ArgumentCaptor<NotificationEntity> entityCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, times(1)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().isRead()).isTrue();
        verify(notificationRepository, never()).countByUserIdAndReadFalse(anyLong());
        verify(notificationRealtimeSseService, never()).publishUnreadCountChanged(anyLong(), anyLong());
    }

    // 동일 게시글 상세를 보고 있지 않으면 기존처럼 알림을 생성하고 카운트를 푸시해야 한다.
    @Test
    void create_comment_reply_should_create_and_publish_when_receiver_not_viewing_article_detail() {
        // Given: 수신자가 동일 게시글 상세를 보고 있지 않은 상태
        UserEntity receiver = createUser(12L, "receiver3");
        UserEntity sender = createUser(22L, "sender3");
        BoardEntity board = createBoard(3L, "general");
        ArticleEntity article = createArticle(32L, board, receiver);
        CommentEntity reply = createComment(42L, article, sender);

        when(notificationPresenceService.isViewingArticleDetail(12L, 32L)).thenReturn(false);
        when(notificationPresenceService.shouldSuppressUnreadCountPush(12L, 32L)).thenReturn(false);
        when(notificationRepository.countByUserIdAndReadFalse(12L)).thenReturn(3L);
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 대댓글 알림 생성 요청
        notificationService.createCommentReply(receiver, sender, article, reply);

        // Then: unread 알림이 생성되고 unread 카운트 푸시가 수행되어야 함
        ArgumentCaptor<NotificationEntity> entityCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, times(1)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().isRead()).isFalse();
        verify(notificationRealtimeSseService, times(1)).publishUnreadCountChanged(12L, 3L);
    }

    private UserEntity createUser(Long id, String suffix) {
        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
        UserEntity user = UserEntity.createLocal(
            role,
            "login-" + suffix,
            suffix + "@test.com",
            "pw",
            "user-" + suffix,
            "display-" + suffix,
            "handle-" + suffix
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private BoardEntity createBoard(Long id, String slug) {
        BoardEntity board = BoardEntity.builder()
            .boardName("board-" + slug)
            .slug(slug)
            .description("테스트 게시판")
            .visibility(BoardVisibility.PUBLIC)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private ArticleEntity createArticle(Long id, BoardEntity board, UserEntity owner) {
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(owner)
            .category(null)
            .visibility(ContentVisibility.PUBLIC)
            .title("title-" + id)
            .content("content-" + id)
            .notice(false)
            .hit(0)
            .build();
        ReflectionTestUtils.setField(article, "id", id);
        return article;
    }

    private CommentEntity createComment(Long id, ArticleEntity article, UserEntity author) {
        CommentEntity comment = CommentEntity.builder()
            .user(author)
            .article(article)
            .parentComment(null)
            .rootComment(null)
            .depth(1)
            .content("reply-" + id)
            .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
