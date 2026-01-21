package com.mocktalkback.domain.moderation.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.article.dto.ArticleCategoryResponse;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.moderation.dto.BoardCategoryCreateRequest;
import com.mocktalkback.domain.moderation.dto.BoardCategoryUpdateRequest;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardCategoryAdminService {

    private final ArticleCategoryRepository articleCategoryRepository;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ArticleCategoryResponse> findAll(Long boardId) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);
        return articleCategoryRepository.findAllByBoardIdOrderByCategoryNameAsc(board.getId()).stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Transactional
    public ArticleCategoryResponse create(Long boardId, BoardCategoryCreateRequest request) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);
        String categoryName = normalizeName(request.categoryName());
        if (articleCategoryRepository.existsByBoardIdAndCategoryNameIgnoreCase(boardId, categoryName)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다.");
        }
        ArticleCategoryEntity entity = ArticleCategoryEntity.builder()
            .board(board)
            .categoryName(categoryName)
            .build();
        ArticleCategoryEntity saved = articleCategoryRepository.save(entity);
        return articleMapper.toResponse(saved);
    }

    @Transactional
    public ArticleCategoryResponse update(Long boardId, Long categoryId, BoardCategoryUpdateRequest request) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);
        ArticleCategoryEntity entity = getCategory(categoryId);
        ensureSameBoard(board, entity);
        String categoryName = normalizeName(request.categoryName());
        if (!entity.getCategoryName().equalsIgnoreCase(categoryName)
            && articleCategoryRepository.existsByBoardIdAndCategoryNameIgnoreCase(boardId, categoryName)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다.");
        }
        entity.updateName(categoryName);
        return articleMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long boardId, Long categoryId) {
        BoardEntity board = getBoard(boardId);
        UserEntity actor = getCurrentUser();
        requireBoardAdmin(actor, board);
        ArticleCategoryEntity entity = getCategory(categoryId);
        ensureSameBoard(board, entity);
        if (articleRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)) {
            throw new IllegalArgumentException("카테고리에 게시글이 존재합니다.");
        }
        articleCategoryRepository.delete(entity);
    }

    private ArticleCategoryEntity getCategory(Long categoryId) {
        return articleCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
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

    private void ensureSameBoard(BoardEntity board, ArticleCategoryEntity category) {
        if (!board.getId().equals(category.getBoard().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판 카테고리가 아닙니다.");
        }
    }

    private String normalizeName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            throw new IllegalArgumentException("카테고리명을 입력해주세요.");
        }
        return categoryName.trim();
    }
}
