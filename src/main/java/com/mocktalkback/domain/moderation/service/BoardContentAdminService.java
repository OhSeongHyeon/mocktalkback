package com.mocktalkback.domain.moderation.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.moderation.dto.BoardAdminArticleItemResponse;
import com.mocktalkback.domain.moderation.dto.BoardAdminCommentItemResponse;
import com.mocktalkback.domain.moderation.type.ReportTargetType;
import com.mocktalkback.domain.moderation.repository.ReportRepository;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardContentAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int COMMENT_PREVIEW_LIMIT = 200;

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<BoardAdminArticleItemResponse> findArticles(
        Long boardId,
        Boolean reported,
        Boolean notice,
        Long authorId,
        int page,
        int size
    ) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        Pageable pageable = toPageable(page, size);
        Page<ArticleEntity> result = articleRepository.findAdminBoardArticles(
            boardId,
            authorId,
            notice,
            reported,
            ReportTargetType.ARTICLE,
            pageable
        );
        List<Long> articleIds = result.getContent().stream()
            .map(ArticleEntity::getId)
            .toList();
        Set<Long> reportedIds = resolveReportedIds(boardId, ReportTargetType.ARTICLE, articleIds);
        Page<BoardAdminArticleItemResponse> mapped = result.map(
            entity -> BoardAdminArticleItemResponse.from(entity, reportedIds.contains(entity.getId()))
        );
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<BoardAdminCommentItemResponse> findComments(
        Long boardId,
        Boolean reported,
        Long authorId,
        int page,
        int size
    ) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);

        Pageable pageable = toPageable(page, size);
        Page<CommentEntity> result = commentRepository.findAdminBoardComments(
            boardId,
            authorId,
            reported,
            ReportTargetType.COMMENT,
            pageable
        );
        List<Long> commentIds = result.getContent().stream()
            .map(CommentEntity::getId)
            .toList();
        Set<Long> reportedIds = resolveReportedIds(boardId, ReportTargetType.COMMENT, commentIds);
        Page<BoardAdminCommentItemResponse> mapped = result.map(
            entity -> BoardAdminCommentItemResponse.from(
                entity,
                reportedIds.contains(entity.getId()),
                truncate(entity.getContent(), COMMENT_PREVIEW_LIMIT)
            )
        );
        return PageResponse.from(mapped);
    }

    @Transactional
    public BoardAdminArticleItemResponse updateNotice(Long boardId, Long articleId, boolean notice) {
        ArticleEntity article = getArticle(articleId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, article.getBoard());
        ensureSameBoard(boardId, article.getBoard());

        article.changeNotice(notice);
        boolean reported = !reportRepository.findAllByBoardIdAndTargetTypeAndTargetIdIn(
            article.getBoard().getId(),
            ReportTargetType.ARTICLE,
            List.of(article.getId())
        ).isEmpty();
        return BoardAdminArticleItemResponse.from(article, reported);
    }

    @Transactional
    public void deleteArticle(Long boardId, Long articleId) {
        ArticleEntity article = getArticle(articleId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, article.getBoard());
        ensureSameBoard(boardId, article.getBoard());

        if (!article.isDeleted()) {
            article.softDelete();
        }
    }

    @Transactional
    public void deleteComment(Long boardId, Long commentId) {
        CommentEntity comment = getComment(commentId);
        UserEntity actor = getCurrentUser();
        BoardEntity board = comment.getArticle().getBoard();
        requireBoardAdmin(actor, board);
        ensureSameBoard(boardId, board);

        if (!comment.isDeleted()) {
            comment.softDelete();
        }
    }

    private Set<Long> resolveReportedIds(Long boardId, ReportTargetType targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>();
        reportRepository.findAllByBoardIdAndTargetTypeAndTargetIdIn(boardId, targetType, targetIds)
            .forEach(report -> result.add(report.getTargetId()));
        return result;
    }

    private ArticleEntity getArticle(Long articleId) {
        return articleRepository.findById(articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private CommentEntity getComment(Long commentId) {
        return commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."));
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private void requireBoardAdmin(UserEntity actor, BoardEntity board) {
        if (RoleNames.ADMIN.equals(actor.getRole().getRoleName())) {
            return;
        }
        BoardMemberEntity member = boardMemberRepository.findByUserIdAndBoardId(actor.getId(), board.getId())
            .orElse(null);
        if (member == null) {
            throw new AccessDeniedException("게시판 관리자 권한이 없습니다.");
        }
        BoardRole role = member.getBoardRole();
        if (role != BoardRole.OWNER && role != BoardRole.MODERATOR) {
            throw new AccessDeniedException("게시판 관리자 권한이 없습니다.");
        }
    }

    private void ensureSameBoard(Long boardId, BoardEntity board) {
        if (!board.getId().equals(boardId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판 콘텐츠가 아닙니다.");
        }
    }

    private Pageable toPageable(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + "...";
    }
}
