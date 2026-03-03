package com.mocktalkback.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.PageNormalizer;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.ImageOptimizationService;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.dto.MyArticleItemResponse;
import com.mocktalkback.domain.user.dto.MyCommentItemResponse;
import com.mocktalkback.domain.user.repository.UserFileRepository;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFileRepository userFileRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileClassRepository fileClassRepository;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private ImageOptimizationService imageOptimizationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private PageNormalizer pageNormalizer = new PageNormalizer();

    @InjectMocks
    private UserService userService;

    // 내 게시글 목록 조회는 마이페이지 전용 DTO를 그대로 반환해야 한다.
    @Test
    void getMyArticles_returns_my_page_article_items() {
        // Given: 로그인 사용자와 마이페이지 게시글 응답
        when(currentUserService.getUserId()).thenReturn(7L);
        MyArticleItemResponse item = new MyArticleItemResponse(
            101L,
            2L,
            "inquiry",
            "문의 게시판",
            7L,
            "Seed2",
            null,
            ContentVisibility.PUBLIC,
            "문의드립니다.",
            12L,
            3L,
            5L,
            1L,
            false,
            Instant.parse("2026-02-01T00:00:00Z"),
            Instant.parse("2026-02-01T00:00:00Z"),
            null
        );
        Page<MyArticleItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1L);
        when(articleRepository.findMyArticleItems(eq(7L), any(Pageable.class))).thenReturn(page);

        // When: 내 게시글 목록을 조회하면
        PageResponse<MyArticleItemResponse> result = userService.getMyArticles(0, 10);

        // Then: 전용 DTO 목록과 페이지 정보가 반환된다.
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).boardSlug()).isEqualTo("inquiry");
        assertThat(result.items().get(0).commentCount()).isEqualTo(3L);
        verify(articleRepository).findMyArticleItems(eq(7L), any(Pageable.class));
    }

    // 내 댓글 목록 조회는 마이페이지 전용 DTO를 그대로 반환해야 한다.
    @Test
    void getMyComments_returns_my_page_comment_items() {
        // Given: 로그인 사용자와 마이페이지 댓글 응답
        when(currentUserService.getUserId()).thenReturn(7L);
        MyCommentItemResponse item = new MyCommentItemResponse(
            501L,
            7L,
            101L,
            "문의드립니다.",
            2L,
            "inquiry",
            "문의 게시판",
            "Seed2",
            null,
            null,
            0,
            "답변 감사합니다.",
            Instant.parse("2026-02-01T00:00:00Z"),
            Instant.parse("2026-02-01T00:00:00Z"),
            null
        );
        Page<MyCommentItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1L);
        when(commentRepository.findMyCommentItems(eq(7L), any(Pageable.class))).thenReturn(page);

        // When: 내 댓글 목록을 조회하면
        PageResponse<MyCommentItemResponse> result = userService.getMyComments(0, 10);

        // Then: 전용 DTO 목록과 페이지 정보가 반환된다.
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).boardName()).isEqualTo("문의 게시판");
        assertThat(result.items().get(0).articleTitle()).isEqualTo("문의드립니다.");
        verify(commentRepository).findMyCommentItems(eq(7L), any(Pageable.class));
    }
}
