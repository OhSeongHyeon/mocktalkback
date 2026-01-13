package com.mocktalkback.domain.article.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.article.dto.ArticleBookmarkCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleBookmarkResponse;
import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.mapper.ArticleMapper;
import com.mocktalkback.domain.article.repository.ArticleBookmarkRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleBookmarkService {

    private final ArticleBookmarkRepository articleBookmarkRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    @Transactional
    public ArticleBookmarkResponse create(ArticleBookmarkCreateRequest request) {
        UserEntity user = getUser(request.userId());
        ArticleEntity article = getArticle(request.articleId());
        ArticleBookmarkEntity entity = articleMapper.toEntity(request, user, article);
        ArticleBookmarkEntity saved = articleBookmarkRepository.save(entity);
        return articleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ArticleBookmarkResponse findById(Long id) {
        ArticleBookmarkEntity entity = articleBookmarkRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("article bookmark not found: " + id));
        return articleMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ArticleBookmarkResponse> findAll() {
        return articleBookmarkRepository.findAll().stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Transactional
    public void delete(Long id) {
        articleBookmarkRepository.deleteById(id);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private ArticleEntity getArticle(Long articleId) {
        return articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("article not found: " + articleId));
    }
}
