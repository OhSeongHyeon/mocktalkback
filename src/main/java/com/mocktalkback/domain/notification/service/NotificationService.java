package com.mocktalkback.domain.notification.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.entity.NotificationEntity;
import com.mocktalkback.domain.notification.repository.NotificationRepository;
import com.mocktalkback.domain.notification.type.NotificationType;
import com.mocktalkback.domain.notification.type.ReferenceType;
import com.mocktalkback.domain.realtime.service.NotificationPresenceService;
import com.mocktalkback.domain.realtime.service.NotificationRealtimeSseService;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.comment.repository.CommentRepository.CommentArticleView;
import com.mocktalkback.domain.article.repository.ArticleRepository.ArticleTitleView;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort DEFAULT_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final NotificationRepository notificationRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;
    private final NotificationPresenceService notificationPresenceService;
    private final NotificationRealtimeSseService notificationRealtimeSseService;

    @Transactional
    public void createArticleComment(
        UserEntity receiver,
        UserEntity sender,
        ArticleEntity article
    ) {
        if (receiver.getId().equals(sender.getId())) {
            return;
        }
        NotificationEntity entity = NotificationEntity.builder()
            .user(receiver)
            .sender(sender)
            .notiType(NotificationType.ARTICLE_COMMENT)
            .redirectUrl(buildArticleRedirect(article))
            .referenceType(ReferenceType.ARTICLE)
            .referenceId(article.getId())
            .read(false)
            .build();
        notificationRepository.save(entity);
        publishUnreadCountChangedAfterCommit(receiver.getId(), article.getId());
    }

    @Transactional
    public void createCommentReply(
        UserEntity receiver,
        UserEntity sender,
        ArticleEntity article,
        CommentEntity comment
    ) {
        if (receiver.getId().equals(sender.getId())) {
            return;
        }
        NotificationEntity entity = NotificationEntity.builder()
            .user(receiver)
            .sender(sender)
            .notiType(NotificationType.COMMENT_REPLY)
            .redirectUrl(buildArticleRedirect(article))
            .referenceType(ReferenceType.COMMENT)
            .referenceId(comment.getId())
            .read(false)
            .build();
        notificationRepository.save(entity);
        publishUnreadCountChangedAfterCommit(receiver.getId(), article.getId());
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        Long userId = currentUserService.getUserId();
        NotificationEntity entity = notificationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found"));
        ArticleTitleBundle titles = resolveArticleTitles(List.of(entity));
        String articleTitle = resolveArticleTitle(entity, titles);
        return toResponse(entity, articleTitle);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findAll(int page, int size, Boolean read) {
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, DEFAULT_SORT);
        Long userId = currentUserService.getUserId();

        Page<NotificationEntity> entities = read == null
            ? notificationRepository.findByUserId(userId, pageable)
            : notificationRepository.findByUserIdAndRead(userId, read, pageable);

        ArticleTitleBundle titles = resolveArticleTitles(entities.getContent());
        Page<NotificationResponse> responsePage = entities.map(
            entity -> toResponse(entity, resolveArticleTitle(entity, titles))
        );
        return PageResponse.from(responsePage);
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Long userId = currentUserService.getUserId();
        NotificationEntity entity = notificationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found"));
        if (!entity.isRead()) {
            entity.updateRead(true);
            publishUnreadCountChangedAfterCommit(userId, null);
        }
        ArticleTitleBundle titles = resolveArticleTitles(List.of(entity));
        String articleTitle = resolveArticleTitle(entity, titles);
        return toResponse(entity, articleTitle);
    }

    @Transactional
    public void markAllRead() {
        Long userId = currentUserService.getUserId();
        notificationRepository.markAllRead(userId);
        publishUnreadCountChangedAfterCommit(userId, null);
    }

    @Transactional
    public void markReadByRedirectUrl(String redirectUrl) {
        Long userId = currentUserService.getUserId();
        String normalizedRedirectUrl = normalizeRedirectUrl(redirectUrl);
        int updatedCount = notificationRepository.markReadByUserIdAndRedirectUrl(userId, normalizedRedirectUrl);
        if (updatedCount > 0) {
            publishUnreadCountChangedAfterCommit(userId, null);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.getUserId();
        NotificationEntity entity = notificationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found"));
        boolean unread = !entity.isRead();
        notificationRepository.delete(entity);
        if (unread) {
            publishUnreadCountChangedAfterCommit(userId, null);
        }
    }

    @Transactional
    public void deleteAll() {
        Long userId = currentUserService.getUserId();
        notificationRepository.deleteAllByUserId(userId);
        publishUnreadCountChangedAfterCommit(userId, null);
    }

    private NotificationResponse toResponse(NotificationEntity entity, String articleTitle) {
        UserEntity sender = entity.getSender();
        String senderName = resolveSenderName(sender);
        String senderHandle = sender == null ? null : sender.getHandle();
        Long senderId = sender == null ? null : sender.getId();
        return new NotificationResponse(
            entity.getId(),
            entity.getUser().getId(),
            senderId,
            senderName,
            senderHandle,
            entity.getNotiType(),
            entity.getRedirectUrl(),
            entity.getReferenceType(),
            entity.getReferenceId(),
            articleTitle,
            entity.isRead(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String buildArticleRedirect(ArticleEntity article) {
        return "/b/" + article.getBoard().getSlug() + "/articles/" + article.getId();
    }

    private String resolveSenderName(UserEntity sender) {
        if (sender == null) {
            return null;
        }
        String displayName = sender.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return sender.getUserName();
    }

    private ArticleTitleBundle resolveArticleTitles(List<NotificationEntity> entities) {
        Set<Long> articleIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();

        for (NotificationEntity entity : entities) {
            if (entity.getReferenceType() == ReferenceType.ARTICLE) {
                articleIds.add(entity.getReferenceId());
            } else if (entity.getReferenceType() == ReferenceType.COMMENT) {
                commentIds.add(entity.getReferenceId());
            }
        }

        Map<Long, Long> commentToArticle = new HashMap<>();
        if (!commentIds.isEmpty()) {
            List<CommentArticleView> mappings = commentRepository.findArticleIdsByCommentIds(commentIds);
            for (CommentArticleView mapping : mappings) {
                commentToArticle.put(mapping.getCommentId(), mapping.getArticleId());
                articleIds.add(mapping.getArticleId());
            }
        }

        Map<Long, String> articleTitles = new HashMap<>();
        if (!articleIds.isEmpty()) {
            List<ArticleTitleView> articles = articleRepository.findTitlesByIdIn(articleIds);
            for (ArticleTitleView article : articles) {
                articleTitles.put(article.getId(), article.getTitle());
            }
        }

        return new ArticleTitleBundle(commentToArticle, articleTitles);
    }

    private String resolveArticleTitle(NotificationEntity entity, ArticleTitleBundle titles) {
        if (entity.getReferenceType() == ReferenceType.ARTICLE) {
            return titles.articleTitles().get(entity.getReferenceId());
        }
        if (entity.getReferenceType() == ReferenceType.COMMENT) {
            Long articleId = titles.commentToArticle().get(entity.getReferenceId());
            if (articleId == null) {
                return null;
            }
            return titles.articleTitles().get(articleId);
        }
        return null;
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return size;
    }

    private String normalizeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("redirectUrl을 입력해주세요.");
        }
        return redirectUrl.trim();
    }

    private void publishUnreadCountChangedAfterCommit(Long userId, Long articleId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishUnreadCountChanged(userId, articleId);
                }
            });
            return;
        }
        publishUnreadCountChanged(userId, articleId);
    }

    private void publishUnreadCountChanged(Long userId, Long articleId) {
        if (notificationPresenceService.shouldSuppressUnreadCountPush(userId, articleId)) {
            return;
        }
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        notificationRealtimeSseService.publishUnreadCountChanged(userId, unreadCount);
    }

    private record ArticleTitleBundle(
        Map<Long, Long> commentToArticle,
        Map<Long, String> articleTitles
    ) {
    }
}
