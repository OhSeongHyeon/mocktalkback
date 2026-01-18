package com.mocktalkback.domain.comment.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentPageResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionSummaryResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionToggleRequest;
import com.mocktalkback.domain.comment.dto.CommentTreeResponse;
import com.mocktalkback.domain.comment.dto.CommentUpdateRequest;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.entity.CommentReactionEntity;
import com.mocktalkback.domain.comment.repository.CommentReactionRepository;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.notification.service.NotificationService;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.util.ReactionTypeValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort ROOT_COMMENT_SORT = Sort.by(
        Sort.Order.asc("createdAt"),
        Sort.Order.asc("id")
    );

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional
    public CommentTreeResponse createRoot(Long articleId, CommentCreateRequest request) {
        UserEntity user = getCurrentUser();
        ArticleEntity article = getAccessibleArticle(articleId, user);
        String content = normalizeContent(request.content());

        CommentEntity entity = CommentEntity.builder()
            .user(user)
            .article(article)
            .parentComment(null)
            .rootComment(null)
            .depth(0)
            .content(content)
            .build();
        CommentEntity saved = commentRepository.save(entity);
        saved.assignRootComment(saved);
        notifyArticleComment(user, article);
        return toTreeResponse(saved);
    }

    @Transactional
    public CommentTreeResponse createReply(Long articleId, Long parentCommentId, CommentCreateRequest request) {
        UserEntity user = getCurrentUser();
        ArticleEntity article = getAccessibleArticle(articleId, user);
        CommentEntity parent = getComment(parentCommentId);
        if (!parent.getArticle().getId().equals(article.getId())) {
            throw new IllegalArgumentException("댓글이 다른 게시글에 속해 있습니다.");
        }

        CommentEntity root = resolveRootComment(parent);
        int depth = parent.getDepth() + 1;
        String content = normalizeContent(request.content());

        CommentEntity entity = CommentEntity.builder()
            .user(user)
            .article(article)
            .parentComment(parent)
            .rootComment(root)
            .depth(depth)
            .content(content)
            .build();
        CommentEntity saved = commentRepository.save(entity);
        notifyCommentReply(user, article, parent, saved);
        return toTreeResponse(saved);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse<CommentTreeResponse> getArticleComments(Long articleId, int page, int size) {
        UserEntity currentUser = currentUserService.getOptionalUserId().map(this::getUser).orElse(null);
        ArticleEntity article = getAccessibleArticle(articleId, currentUser);
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, ROOT_COMMENT_SORT);

        Page<CommentEntity> rootPage = commentRepository.findByArticleIdAndParentCommentIsNull(
            article.getId(),
            pageable
        );
        List<CommentEntity> rootComments = rootPage.getContent();
        List<Long> rootIds = rootComments.stream()
            .map(CommentEntity::getId)
            .toList();

        Map<Long, CommentTreeResponse> rootNodeMap = new LinkedHashMap<>();
        if (!rootIds.isEmpty()) {
            List<CommentEntity> treeEntities = commentRepository.findTreeByArticleIdAndRootIds(
                article.getId(),
                rootIds
            );
            List<Long> commentIds = treeEntities.stream()
                .map(CommentEntity::getId)
                .toList();
            Map<Long, ReactionCounts> reactionCounts = getReactionCounts(commentIds);
            Map<Long, Short> myReactions = getMyReactions(currentUser, commentIds);

            Map<Long, CommentTreeResponse> nodeMap = new LinkedHashMap<>();
            for (CommentEntity entity : treeEntities) {
                ReactionCounts counts = reactionCounts.getOrDefault(entity.getId(), ReactionCounts.empty());
                short myReaction = myReactions.getOrDefault(entity.getId(), (short) 0);
                nodeMap.put(entity.getId(), toTreeResponse(entity, counts, myReaction));
            }
            for (CommentEntity entity : treeEntities) {
                CommentTreeResponse node = nodeMap.get(entity.getId());
                if (node == null) {
                    continue;
                }
                CommentEntity parent = entity.getParentComment();
                if (parent != null && nodeMap.containsKey(parent.getId())) {
                    nodeMap.get(parent.getId()).children().add(node);
                }
            }
            for (CommentEntity root : rootComments) {
                CommentTreeResponse node = nodeMap.get(root.getId());
                if (node != null) {
                    rootNodeMap.put(root.getId(), node);
                }
            }
        }

        Page<CommentTreeResponse> treePage = rootPage.map(root -> {
            CommentTreeResponse node = rootNodeMap.get(root.getId());
            if (node != null) {
                return node;
            }
            return toTreeResponse(root, ReactionCounts.empty(), (short) 0);
        });

        return CommentPageResponse.from(treePage);
    }

    @Transactional
    public CommentReactionSummaryResponse toggleReaction(Long commentId, CommentReactionToggleRequest request) {
        if (request.reactionType() == null) {
            throw new IllegalArgumentException("reactionType은 필수입니다.");
        }
        short reactionType = request.reactionType();
        ReactionTypeValidator.validate(reactionType);
        if (reactionType == 0) {
            throw new IllegalArgumentException("reactionType은 -1 또는 1만 허용됩니다.");
        }

        UserEntity user = getCurrentUser();
        CommentEntity comment = getComment(commentId);
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글에는 반응할 수 없습니다.");
        }
        getAccessibleArticle(comment.getArticle().getId(), user);

        CommentReactionEntity existing = commentReactionRepository
            .findByUserIdAndCommentId(user.getId(), comment.getId())
            .orElse(null);

        short myReaction = reactionType;
        if (existing == null) {
            CommentReactionEntity created = CommentReactionEntity.builder()
                .user(user)
                .comment(comment)
                .reactionType(reactionType)
                .build();
            commentReactionRepository.save(created);
        } else if (existing.getReactionType() == reactionType) {
            commentReactionRepository.delete(existing);
            myReaction = 0;
        } else {
            existing.updateReactionType(reactionType);
        }

        ReactionCounts counts = getReactionCounts(comment.getId());
        return new CommentReactionSummaryResponse(
            comment.getId(),
            counts.likeCount(),
            counts.dislikeCount(),
            myReaction
        );
    }

    @Transactional
    public CommentTreeResponse update(Long id, CommentUpdateRequest request) {
        CommentEntity entity = getComment(id);
        UserEntity user = getCurrentUser();
        if (entity.isDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글은 수정할 수 없습니다.");
        }
        requireOwnership(user, entity);
        entity.updateContent(normalizeContent(request.content()));
        return toTreeResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        CommentEntity entity = getComment(id);
        UserEntity user = getCurrentUser();
        requireOwnership(user, entity);
        if (!entity.isDeleted()) {
            entity.softDelete();
        }
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return getUser(userId);
    }

    private CommentEntity getComment(Long commentId) {
        return commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
    }

    private ArticleEntity getAccessibleArticle(Long articleId, UserEntity currentUser) {
        ArticleEntity article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "article not found"));
        BoardEntity board = article.getBoard();

        UserEntity user = currentUser;
        if (user == null) {
            Optional<Long> optionalUserId = currentUserService.getOptionalUserId();
            user = optionalUserId.map(this::getUser).orElse(null);
        }
        BoardMemberEntity member = user == null
            ? null
            : boardMemberRepository.findByUserIdAndBoardId(user.getId(), board.getId()).orElse(null);

        if (!canAccessBoard(board, user, member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }
        EnumSet<ContentVisibility> allowed = resolveAllowedVisibilities(board, user, member);
        if (!allowed.contains(article.getVisibility())) {
            throw new AccessDeniedException("댓글 조회 권한이 없습니다.");
        }
        return article;
    }

    private CommentEntity resolveRootComment(CommentEntity parent) {
        CommentEntity root = parent.getRootComment();
        if (root != null) {
            return root;
        }
        return parent;
    }

    private CommentTreeResponse toTreeResponse(CommentEntity entity) {
        return toTreeResponse(entity, ReactionCounts.empty(), (short) 0);
    }

    private CommentTreeResponse toTreeResponse(CommentEntity entity, ReactionCounts counts, short myReaction) {
        String content = entity.isDeleted() ? "삭제된 댓글입니다." : entity.getContent();
        Long parentId = entity.getParentComment() == null ? null : entity.getParentComment().getId();
        Long rootId = entity.getRootComment() == null ? entity.getId() : entity.getRootComment().getId();
        return new CommentTreeResponse(
            entity.getId(),
            entity.getUser().getId(),
            resolveAuthorName(entity.getUser()),
            content,
            entity.getDepth(),
            parentId,
            rootId,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getDeletedAt(),
            counts.likeCount(),
            counts.dislikeCount(),
            myReaction,
            new ArrayList<>()
        );
    }

    private void requireOwnership(UserEntity user, CommentEntity entity) {
        if (entity.getUser().getId().equals(user.getId())) {
            return;
        }
        if (isManagerOrAdmin(user)) {
            return;
        }
        throw new AccessDeniedException("댓글 수정/삭제 권한이 없습니다.");
    }

    private String normalizeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        return content.trim();
    }

    private void notifyArticleComment(UserEntity sender, ArticleEntity article) {
        UserEntity receiver = article.getUser();
        if (receiver.getId().equals(sender.getId())) {
            return;
        }
        notificationService.createArticleComment(receiver, sender, article);
    }

    private void notifyCommentReply(
        UserEntity sender,
        ArticleEntity article,
        CommentEntity parent,
        CommentEntity reply
    ) {
        UserEntity receiver = parent.getUser();
        if (receiver.getId().equals(sender.getId())) {
            return;
        }
        notificationService.createCommentReply(receiver, sender, article, reply);
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

    private ReactionCounts getReactionCounts(Long commentId) {
        long likeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, (short) 1);
        long dislikeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, (short) -1);
        return new ReactionCounts(likeCount, dislikeCount);
    }

    private Map<Long, ReactionCounts> getReactionCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReactionCounts> counts = new HashMap<>();
        List<CommentReactionRepository.CommentReactionCountView> views =
            commentReactionRepository.countByCommentIds(commentIds);
        for (CommentReactionRepository.CommentReactionCountView view : views) {
            ReactionCounts current = counts.getOrDefault(view.getCommentId(), ReactionCounts.empty());
            if (view.getReactionType() == 1) {
                counts.put(view.getCommentId(), new ReactionCounts(view.getCount(), current.dislikeCount()));
            } else if (view.getReactionType() == -1) {
                counts.put(view.getCommentId(), new ReactionCounts(current.likeCount(), view.getCount()));
            }
        }
        return counts;
    }

    private Map<Long, Short> getMyReactions(UserEntity user, List<Long> commentIds) {
        if (user == null || commentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Short> result = new HashMap<>();
        List<CommentReactionRepository.CommentReactionUserView> views =
            commentReactionRepository.findUserReactions(user.getId(), commentIds);
        for (CommentReactionRepository.CommentReactionUserView view : views) {
            result.put(view.getCommentId(), view.getReactionType());
        }
        return result;
    }

    private boolean canAccessBoard(BoardEntity board, UserEntity user, BoardMemberEntity member) {
        BoardVisibility visibility = board.getVisibility();
        if (user == null) {
            return visibility == BoardVisibility.PUBLIC;
        }
        if (isManagerOrAdmin(user)) {
            return true;
        }
        if (member != null && member.getBoardRole() == BoardRole.BANNED) {
            return false;
        }
        if (visibility == BoardVisibility.PUBLIC || visibility == BoardVisibility.GROUP) {
            return true;
        }
        if (visibility == BoardVisibility.PRIVATE) {
            return member != null && member.getBoardRole() == BoardRole.OWNER;
        }
        return false;
    }

    private EnumSet<ContentVisibility> resolveAllowedVisibilities(
        BoardEntity board,
        UserEntity user,
        BoardMemberEntity member
    ) {
        if (user == null) {
            return EnumSet.of(ContentVisibility.PUBLIC);
        }
        if (isManagerOrAdmin(user)) {
            return EnumSet.allOf(ContentVisibility.class);
        }
        BoardVisibility visibility = board.getVisibility();
        if (visibility == BoardVisibility.UNLISTED) {
            return EnumSet.noneOf(ContentVisibility.class);
        }
        if (visibility == BoardVisibility.PRIVATE) {
            if (member != null && member.getBoardRole() == BoardRole.OWNER) {
                return EnumSet.of(ContentVisibility.PUBLIC, ContentVisibility.MEMBERS, ContentVisibility.MODERATORS);
            }
            return EnumSet.noneOf(ContentVisibility.class);
        }
        EnumSet<ContentVisibility> allowed = EnumSet.of(ContentVisibility.PUBLIC);
        if (visibility == BoardVisibility.PUBLIC) {
            allowed.add(ContentVisibility.MEMBERS);
        }
        if (visibility == BoardVisibility.GROUP && isActiveMember(member)) {
            allowed.add(ContentVisibility.MEMBERS);
        }
        if (member != null && (member.getBoardRole() == BoardRole.OWNER || member.getBoardRole() == BoardRole.MODERATOR)) {
            allowed.add(ContentVisibility.MODERATORS);
        }
        return allowed;
    }

    private boolean isActiveMember(BoardMemberEntity member) {
        if (member == null) {
            return false;
        }
        BoardRole role = member.getBoardRole();
        return role == BoardRole.OWNER || role == BoardRole.MODERATOR || role == BoardRole.MEMBER;
    }

    private boolean isManagerOrAdmin(UserEntity user) {
        String roleName = user.getRole().getRoleName();
        return RoleNames.MANAGER.equals(roleName) || RoleNames.ADMIN.equals(roleName);
    }

    private String resolveAuthorName(UserEntity user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return user.getUserName();
    }

    private record ReactionCounts(long likeCount, long dislikeCount) {
        private static ReactionCounts empty() {
            return new ReactionCounts(0, 0);
        }
    }
}
