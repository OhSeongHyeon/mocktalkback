package com.mocktalkback.domain.article.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleImportExecuteItemResponse;
import com.mocktalkback.domain.article.dto.ArticleImportExecuteResponse;
import com.mocktalkback.domain.article.dto.ArticleImportPreviewItemResponse;
import com.mocktalkback.domain.article.dto.ArticleImportPreviewResponse;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.repository.ArticleCategoryRepository;
import com.mocktalkback.domain.article.service.ArticleImportBundleParser.ArticleImportBundle;
import com.mocktalkback.domain.article.service.ArticleImportBundleParser.ArticleImportCandidate;
import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.common.policy.BoardAccessPolicy;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleImportService {

    private final ArticleImportBundleParser articleImportBundleParser;
    private final ArticleService articleService;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final UserRepository userRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public ArticleImportPreviewResponse preview(MultipartFile file, boolean autoCreateMissingCategories) {
        ArticleImportBundle bundle = articleImportBundleParser.parse(file);
        UserEntity actor = getCurrentUser();
        List<PreparedImportArticle> articles = prepareArticles(bundle, actor, autoCreateMissingCategories);
        int executableCount = (int) articles.stream().filter(PreparedImportArticle::executable).count();
        List<ArticleImportPreviewItemResponse> items = articles.stream()
            .map(article -> new ArticleImportPreviewItemResponse(
                article.filePath(),
                article.title(),
                article.boardSlug(),
                article.categoryName(),
                article.visibility(),
                article.executable(),
                article.warnings(),
                article.errors()
            ))
            .toList();
        return new ArticleImportPreviewResponse(
            executableCount > 0,
            items.size(),
            executableCount,
            items.size() - executableCount,
            items
        );
    }

    public ArticleImportExecuteResponse execute(MultipartFile file, boolean autoCreateMissingCategories) {
        ArticleImportBundle bundle = articleImportBundleParser.parse(file);
        UserEntity actor = getCurrentUser();
        List<PreparedImportArticle> articles = prepareArticles(bundle, actor, autoCreateMissingCategories);

        List<ArticleImportExecuteItemResponse> items = new ArrayList<>();
        int successCount = 0;
        for (PreparedImportArticle article : articles) {
            List<String> warnings = new ArrayList<>(article.warnings());
            List<String> errors = new ArrayList<>(article.errors());
            Long createdArticleId = null;
            boolean created = false;
            Long categoryId = article.categoryId();

            if (article.executable()) {
                try {
                    if (categoryId == null && autoCreateMissingCategories && article.boardId() != null && article.categoryName() != null) {
                        categoryId = ensureCategory(article.boardId(), article.categoryName());
                    }
                    ArticleResponse response = articleService.create(new ArticleCreateRequest(
                        article.boardId(),
                        actor.getId(),
                        categoryId,
                        article.visibilityEnum(),
                        article.title(),
                        article.contentSource(),
                        ArticleContentFormat.MARKDOWN,
                        false,
                        List.of()
                    ));
                    createdArticleId = response.id();
                    created = true;
                    successCount += 1;
                } catch (RuntimeException exception) {
                    errors.add(resolveExecutionErrorMessage(exception));
                }
            }

            items.add(new ArticleImportExecuteItemResponse(
                article.filePath(),
                article.title(),
                article.boardSlug(),
                article.categoryName(),
                article.visibility(),
                created,
                createdArticleId,
                warnings,
                errors
            ));
        }

        return new ArticleImportExecuteResponse(items.size(), successCount, items.size() - successCount, items);
    }

    private List<PreparedImportArticle> prepareArticles(
        ArticleImportBundle bundle,
        UserEntity actor,
        boolean autoCreateMissingCategories
    ) {
        List<PreparedImportArticle> prepared = new ArrayList<>();
        for (ArticleImportCandidate candidate : bundle.articles()) {
            List<String> warnings = new ArrayList<>(candidate.warnings());
            List<String> errors = new ArrayList<>(candidate.errors());

            String title = normalizeText(candidate.title());
            if (title == null) {
                errors.add("제목을 확인할 수 없습니다.");
            } else if (title.length() > 255) {
                errors.add("제목은 255자를 초과할 수 없습니다.");
            }

            String contentSource = candidate.contentSource();
            String bodyContent = stripMarkdownFrontmatter(contentSource);
            if (bodyContent == null || bodyContent.isBlank()) {
                errors.add("본문이 비어 있습니다.");
            }

            String boardSlug = normalizeText(candidate.boardSlug());
            String categoryName = normalizeText(candidate.categoryName());
            BoardEntity board = null;
            BoardMemberEntity member = null;
            ArticleCategoryEntity category = null;
            if (boardSlug == null) {
                errors.add("대상 게시판 slug가 없습니다.");
            } else {
                board = boardRepository.findBySlugAndDeletedAtIsNull(boardSlug)
                    .orElse(null);
                if (board == null) {
                    errors.add("게시판을 찾을 수 없습니다: " + boardSlug);
                } else {
                    member = boardMemberRepository.findByUserIdAndBoardId(actor.getId(), board.getId())
                        .orElse(null);
                    if (!boardAccessPolicy.canAccessBoard(board, actor, member)) {
                        errors.add("게시판에 접근할 수 없습니다: " + boardSlug);
                    } else {
                        try {
                            boardAccessPolicy.requireCanWrite(board, actor, member);
                        } catch (AccessDeniedException exception) {
                            errors.add(exception.getMessage());
                        }

                        if (categoryName != null) {
                            category = articleCategoryRepository.findByBoardIdAndCategoryNameIgnoreCase(board.getId(), categoryName)
                                .orElse(null);
                            if (category == null) {
                                if (autoCreateMissingCategories) {
                                    warnings.add("게시판 카테고리가 없어 실행 시 자동 생성합니다: " + categoryName);
                                } else {
                                    errors.add("게시판 카테고리를 찾을 수 없습니다: " + categoryName);
                                }
                            }
                        }
                    }
                }
            }

            ContentVisibility visibility = null;
            String visibilityName = normalizeText(candidate.visibility());
            if (visibilityName == null) {
                visibilityName = ContentVisibility.PUBLIC.name();
            }
            try {
                visibility = ContentVisibility.valueOf(visibilityName.toUpperCase());
            } catch (IllegalArgumentException exception) {
                errors.add("지원하지 않는 공개 범위입니다: " + visibilityName);
            }

            if (board != null && visibility != null) {
                EnumSet<ContentVisibility> allowed = boardAccessPolicy.resolveAllowedVisibilities(board, actor, member);
                if (!allowed.contains(visibility)) {
                    errors.add("현재 계정으로 선택할 수 없는 공개 범위입니다: " + visibility.name());
                }
            }

            prepared.add(new PreparedImportArticle(
                candidate.filePath(),
                title,
                boardSlug,
                categoryName,
                visibilityName != null ? visibilityName.toUpperCase() : null,
                contentSource,
                board != null ? board.getId() : null,
                category != null ? category.getId() : null,
                visibility,
                warnings,
                errors,
                errors.isEmpty()
            ));
        }
        return prepared;
    }

    private Long ensureCategory(Long boardId, String categoryName) {
        ArticleCategoryEntity existing = articleCategoryRepository.findByBoardIdAndCategoryNameIgnoreCase(boardId, categoryName)
            .orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        BoardEntity board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + boardId));

        try {
            ArticleCategoryEntity created = articleCategoryRepository.save(
                ArticleCategoryEntity.builder()
                    .board(board)
                    .categoryName(categoryName)
                    .build()
            );
            return created.getId();
        } catch (DataIntegrityViolationException exception) {
            return articleCategoryRepository.findByBoardIdAndCategoryNameIgnoreCase(boardId, categoryName)
                .map(ArticleCategoryEntity::getId)
                .orElseThrow(() -> exception);
        }
    }

    private UserEntity getCurrentUser() {
        Long userId = currentUserService.getUserId();
        return userRepository.findByIdWithRoleAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private String resolveExecutionErrorMessage(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getReason() == null
                ? "게시글 생성에 실패했습니다."
                : responseStatusException.getReason();
        }
        String message = normalizeText(exception.getMessage());
        return message == null ? "게시글 생성에 실패했습니다." : message;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stripMarkdownFrontmatter(String contentSource) {
        if (contentSource == null || contentSource.isBlank()) {
            return contentSource;
        }

        String normalized = contentSource.startsWith("\uFEFF") ? contentSource.substring(1) : contentSource;
        String[] lines = normalized.split("\\R", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) {
            return normalized;
        }

        int closingIndex = -1;
        for (int index = 1; index < lines.length; index += 1) {
            String line = lines[index].trim();
            if ("---".equals(line) || "...".equals(line)) {
                closingIndex = index;
                break;
            }
        }

        if (closingIndex < 0) {
            return normalized;
        }

        String body = String.join("\n", List.of(lines).subList(closingIndex + 1, lines.length));
        return body.replaceFirst("^(\\r?\\n)+", "");
    }

    private record PreparedImportArticle(
        String filePath,
        String title,
        String boardSlug,
        String categoryName,
        String visibility,
        String contentSource,
        Long boardId,
        Long categoryId,
        ContentVisibility visibilityEnum,
        List<String> warnings,
        List<String> errors,
        boolean executable
    ) {
    }
}
