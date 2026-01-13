package com.mocktalkback.domain.comment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentResponse;
import com.mocktalkback.domain.comment.dto.CommentUpdateRequest;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.mapper.CommentMapper;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {
        UserEntity user = getUser(request.userId());
        ArticleEntity article = getArticle(request.articleId());
        CommentEntity parent = getComment(request.parentCommentId());
        CommentEntity root = getComment(request.rootCommentId());
        CommentEntity entity = commentMapper.toEntity(request, user, article, parent, root);
        CommentEntity saved = commentRepository.save(entity);
        return commentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CommentResponse findById(Long id) {
        CommentEntity entity = commentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("comment not found: " + id));
        return commentMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findAll() {
        return commentRepository.findAll().stream()
            .map(commentMapper::toResponse)
            .toList();
    }

    @Transactional
    public CommentResponse update(Long id, CommentUpdateRequest request) {
        CommentEntity entity = commentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("comment not found: " + id));
        entity.updateContent(request.content());
        return commentMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        commentRepository.deleteById(id);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private ArticleEntity getArticle(Long articleId) {
        return articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("article not found: " + articleId));
    }

    private CommentEntity getComment(Long commentId) {
        if (commentId == null) {
            return null;
        }
        return commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("comment not found: " + commentId));
    }
}
