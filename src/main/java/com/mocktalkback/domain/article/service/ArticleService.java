package com.mocktalkback.domain.article.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.article.dto.ArticleBoardResponse;
import com.mocktalkback.domain.article.dto.ArticleDetailResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionSummaryResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionToggleRequest;
import com.mocktalkback.domain.article.dto.ArticleSummaryResponse;
import com.mocktalkback.domain.article.dto.BoardArticleListResponse;
import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleFileEntity;
import com.mocktalkback.domain.article.entity.ArticleReactionEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.article.repository.ArticleFileRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository.ArticleReactionCountView;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardFileRepository;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;
import com.mocktalkback.global.common.sanitize.HtmlSanitizer;
import com.mocktalkback.global.common.util.ReactionTypeValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int PINNED_LIMIT = 5;
    private static final Sort ARTICLE_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("id")
    );

    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardFileRepository boardFileRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final ArticleFileRepository articleFileRepository;
    private final ArticleReactionRepository articleReactionRepository;
    private final ArticleMapper articleMapper;
    private final FileMapper fileMapper;
    private final CurrentUserService currentUserService;
    private final HtmlSanitizer htmlSanitizer;

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        BoardEntity board = getBoard(request.boardId());
        UserEntity user = getUser(request.userId());
        ArticleCategoryEntity category = getCategory(request.categoryId());
        String sanitizedContent = htmlSanitizer.sanitize(request.content());
        ArticleCreateRequest sanitizedRequest = new ArticleCreateRequest(
            request.boardId(),
            request.userId(),
            request.categoryId(),
            request.visibility(),
            request.title(),
            sanitizedContent,
            request.notice()
        );
        ArticleEntity entity = articleMapper.toEntity(sanitizedRequest, board, user, category);
        ArticleEntity saved = articleRepository.save(entity);
        return articleMapper.toResponse(saved);
    }

    @Transactional
    public ArticleDetailResponse findDetailById(Long id, boolean increaseHit) {
        ArticleEntity article = articleRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "article not found"));

        BoardEntity board = article.getBoard();
        Long userId = currentUserService.getOptionalUserId().orElse(null);
        UserEntity user = userId == null ? null : getUser(userId);
        BoardMemberEntity member = userId == null
            ? null
            : boardMemberRepository.findByUserIdAndBoardId(userId, board.getId()).orElse(null);

        if (!canAccessBoard(board, user, member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }

        EnumSet<ContentVisibility> allowed = resolveAllowedVisibilities(board, user, member);
        if (!allowed.contains(article.getVisibility())) {
            throw new AccessDeniedException("게시글 조회 권한이 없습니다.");
        }

        if (increaseHit) {
            article.increaseHit();
        }

        long commentCount = getCommentCount(article.getId());
        ReactionCounts reactionCounts = getReactionCounts(article.getId());
        short myReaction = resolveMyReaction(article.getId(), user);
        List<FileResponse> attachments = resolveAttachments(article.getId());
        FileResponse boardImage = resolveBoardImage(board.getId());
        ArticleBoardResponse boardResponse = new ArticleBoardResponse(
            board.getId(),
            board.getBoardName(),
            board.getSlug(),
            board.getDescription(),
            board.getVisibility(),
            boardImage
        );

        return new ArticleDetailResponse(
            article.getId(),
            boardResponse,
            article.getUser().getId(),
            resolveAuthorName(article.getUser()),
            article.getVisibility(),
            article.getTitle(),
            article.getContent(),
            article.getHit(),
            commentCount,
            reactionCounts.likeCount(),
            reactionCounts.dislikeCount(),
            myReaction,
            article.isNotice(),
            article.getCreatedAt(),
            article.getUpdatedAt(),
            attachments
        );
    }

    @Transactional
    public ArticleReactionSummaryResponse toggleReaction(Long articleId, ArticleReactionToggleRequest request) {
        if (request.reactionType() == null) {
            throw new IllegalArgumentException("reactionType은 필수입니다.");
        }
        short reactionType = request.reactionType();
        ReactionTypeValidator.validate(reactionType);
        if (reactionType == 0) {
            throw new IllegalArgumentException("reactionType은 -1 또는 1만 허용됩니다.");
        }

        UserEntity user = getCurrentUser();
        ArticleEntity article = getArticleForReaction(articleId, user);

        ArticleReactionEntity existing = articleReactionRepository
            .findByUserIdAndArticleId(user.getId(), article.getId())
            .orElse(null);

        short myReaction = reactionType;
        if (existing == null) {
            ArticleReactionEntity created = ArticleReactionEntity.builder()
                .user(user)
                .article(article)
                .reactionType(reactionType)
                .build();
            articleReactionRepository.save(created);
        } else if (existing.getReactionType() == reactionType) {
            articleReactionRepository.delete(existing);
            myReaction = 0;
        } else {
            existing.updateReactionType(reactionType);
        }

        ReactionCounts counts = getReactionCounts(article.getId());
        return new ArticleReactionSummaryResponse(
            article.getId(),
            counts.likeCount(),
            counts.dislikeCount(),
            myReaction
        );
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findAll() {
        return articleRepository.findAll().stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BoardArticleListResponse getBoardArticles(Long boardId, int page, int size) {
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, ARTICLE_SORT);

        BoardEntity board = getBoardForRead(boardId);
        Long userId = currentUserService.getOptionalUserId().orElse(null);
        UserEntity user = userId == null ? null : getUser(userId);
        BoardMemberEntity member = userId == null
            ? null
            : boardMemberRepository.findByUserIdAndBoardId(userId, boardId).orElse(null);

        if (!canAccessBoard(board, user, member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }

        EnumSet<ContentVisibility> visibilities = resolveAllowedVisibilities(board, user, member);
        if (visibilities.isEmpty()) {
            throw new AccessDeniedException("게시글 조회 권한이 없습니다.");
        }

        Page<ArticleEntity> pageResult = articleRepository.findByBoardIdAndNoticeFalseAndVisibilityInAndDeletedAtIsNull(
            boardId,
            visibilities,
            pageable
        );

        List<ArticleEntity> pinnedEntities = List.of();
        if (resolvedPage == 0) {
            pinnedEntities = articleRepository.findByBoardIdAndNoticeTrueAndVisibilityInAndDeletedAtIsNull(
                boardId,
                visibilities,
                PageRequest.of(0, PINNED_LIMIT, ARTICLE_SORT)
            );
        }

        Map<Long, Long> commentCounts = loadCommentCounts(pageResult.getContent(), pinnedEntities);
        Map<Long, ReactionCounts> reactionCounts = loadReactionCounts(pageResult.getContent(), pinnedEntities);

        List<ArticleSummaryResponse> pinned = mapSummaries(pinnedEntities, commentCounts, reactionCounts);
        List<ArticleSummaryResponse> items = mapSummaries(pageResult.getContent(), commentCounts, reactionCounts);

        PageResponse<ArticleSummaryResponse> pageResponse = new PageResponse<>(
            items,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.hasNext(),
            pageResult.hasPrevious()
        );

        return new BoardArticleListResponse(pinned, pageResponse);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleUpdateRequest request) {
        ArticleEntity entity = articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("article not found: " + id));
        ArticleCategoryEntity category = getCategory(request.categoryId());
        String sanitizedContent = htmlSanitizer.sanitize(request.content());
        entity.update(category, request.visibility(), request.title(), sanitizedContent, request.notice());
        return articleMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        ArticleEntity entity = articleRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "article not found"));
        UserEntity user = getCurrentUser();
        requireOwnership(user, entity);
        if (!entity.isDeleted()) {
            entity.softDelete();
        }
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + boardId));
    }

    private BoardEntity getBoardForRead(Long boardId) {
        return boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found"));
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return getUser(userId);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private ArticleCategoryEntity getCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return articleCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("category not found: " + categoryId));
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

    private void requireOwnership(UserEntity user, ArticleEntity entity) {
        if (entity.getUser().getId().equals(user.getId())) {
            return;
        }
        if (isManagerOrAdmin(user)) {
            return;
        }
        throw new AccessDeniedException("게시글 삭제 권한이 없습니다.");
    }

    private Map<Long, Long> loadCommentCounts(
        List<ArticleEntity> items,
        List<ArticleEntity> pinned
    ) {
        Set<Long> ids = new LinkedHashSet<>();
        items.forEach(article -> ids.add(article.getId()));
        pinned.forEach(article -> ids.add(article.getId()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countByArticleIds(ids).stream()
            .collect(Collectors.toMap(
                CommentRepository.CommentCountView::getArticleId,
                CommentRepository.CommentCountView::getCount
            ));
    }

    private Map<Long, ReactionCounts> loadReactionCounts(
        List<ArticleEntity> items,
        List<ArticleEntity> pinned
    ) {
        List<ArticleEntity> combined = new ArrayList<>(items);
        combined.addAll(pinned);
        List<Long> articleIds = combined.stream()
            .map(ArticleEntity::getId)
            .toList();
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ReactionCounts> counts = new HashMap<>();
        List<ArticleReactionCountView> result = articleReactionRepository.countByArticleIds(articleIds);
        for (ArticleReactionCountView row : result) {
            ReactionCounts current = counts.getOrDefault(row.getArticleId(), ReactionCounts.empty());
            if (row.getReactionType() == 1) {
                counts.put(row.getArticleId(), new ReactionCounts(current.likeCount() + row.getCount(), current.dislikeCount()));
            } else if (row.getReactionType() == -1) {
                counts.put(row.getArticleId(), new ReactionCounts(current.likeCount(), current.dislikeCount() + row.getCount()));
            }
        }
        return counts;
    }

    private long getCommentCount(Long articleId) {
        return commentRepository.countByArticleIds(List.of(articleId)).stream()
            .findFirst()
            .map(CommentRepository.CommentCountView::getCount)
            .orElse(0L);
    }

    private ReactionCounts getReactionCounts(Long articleId) {
        long likeCount = articleReactionRepository.countByArticleIdAndReactionType(articleId, (short) 1);
        long dislikeCount = articleReactionRepository.countByArticleIdAndReactionType(articleId, (short) -1);
        return new ReactionCounts(likeCount, dislikeCount);
    }

    private short resolveMyReaction(Long articleId, UserEntity user) {
        if (user == null) {
            return 0;
        }
        return articleReactionRepository.findByUserIdAndArticleId(user.getId(), articleId)
            .map(ArticleReactionEntity::getReactionType)
            .orElse((short) 0);
    }

    private ArticleEntity getArticleForReaction(Long articleId, UserEntity user) {
        ArticleEntity article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "article not found"));

        BoardEntity board = article.getBoard();
        BoardMemberEntity member = boardMemberRepository
            .findByUserIdAndBoardId(user.getId(), board.getId())
            .orElse(null);

        if (!canAccessBoard(board, user, member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found");
        }

        EnumSet<ContentVisibility> allowed = resolveAllowedVisibilities(board, user, member);
        if (!allowed.contains(article.getVisibility())) {
            throw new AccessDeniedException("게시글 반응 권한이 없습니다.");
        }
        return article;
    }

    private List<FileResponse> resolveAttachments(Long articleId) {
        List<ArticleFileEntity> mappings = articleFileRepository.findAllByArticleIdOrderByCreatedAtAsc(articleId);
        return mappings.stream()
            .map(ArticleFileEntity::getFile)
            .filter(file -> !file.isDeleted())
            .map(fileMapper::toResponse)
            .toList();
    }

    private FileResponse resolveBoardImage(Long boardId) {
        List<BoardFileEntity> files = boardFileRepository.findAllByBoardIdOrderByCreatedAtDesc(boardId);
        for (BoardFileEntity boardFile : files) {
            FileEntity file = boardFile.getFile();
            if (file.isDeleted()) {
                continue;
            }
            return fileMapper.toResponse(file);
        }
        return null;
    }

    private List<ArticleSummaryResponse> mapSummaries(
        List<ArticleEntity> articles,
        Map<Long, Long> commentCounts,
        Map<Long, ReactionCounts> reactionCounts
    ) {
        return articles.stream()
            .map(article -> new ArticleSummaryResponse(
                article.getId(),
                article.getBoard().getId(),
                article.getUser().getId(),
                resolveAuthorName(article.getUser()),
                article.getTitle(),
                article.getHit(),
                commentCounts.getOrDefault(article.getId(), 0L),
                reactionCounts.getOrDefault(article.getId(), ReactionCounts.empty()).likeCount(),
                reactionCounts.getOrDefault(article.getId(), ReactionCounts.empty()).dislikeCount(),
                article.isNotice(),
                article.getCreatedAt()
            ))
            .toList();
    }

    private String resolveAuthorName(UserEntity user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return user.getUserName();
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

    private record ReactionCounts(long likeCount, long dislikeCount) {
        private static ReactionCounts empty() {
            return new ReactionCounts(0, 0);
        }
    }
}
