package com.mocktalkback.domain.article.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final ArticleMapper articleMapper;

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        BoardEntity board = getBoard(request.boardId());
        UserEntity user = getUser(request.userId());
        ArticleCategoryEntity category = getCategory(request.categoryId());
        ArticleEntity entity = articleMapper.toEntity(request, board, user, category);
        ArticleEntity saved = articleRepository.save(entity);
        return articleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ArticleResponse findById(Long id) {
        ArticleEntity entity = articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("article not found: " + id));
        return articleMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findAll() {
        return articleRepository.findAll().stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleUpdateRequest request) {
        ArticleEntity entity = articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("article not found: " + id));
        ArticleCategoryEntity category = getCategory(request.categoryId());
        entity.update(category, request.visibility(), request.title(), request.content(), request.notice());
        return articleMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + boardId));
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
}
