package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.global.common.sanitize.HtmlSanitizer;
import com.mocktalkback.global.common.sanitize.HtmlSanitizerProperties;

class ArticleContentServiceTest {

    // Markdown 원본은 HTML로 렌더링하고 원본 텍스트는 그대로 유지해야 한다.
    @Test
    void render_markdown_returns_rendered_html_and_preserves_source() {
        // Given: Markdown 렌더 서비스와 원본 텍스트
        ArticleContentService articleContentService = new ArticleContentService(createSanitizer());
        String markdown = """
            # 제목

            - [x] 완료

            | 항목 | 값 |
            | --- | --- |
            | a | b |
            """;

        // When: Markdown을 HTML로 렌더링하면
        ArticleContentService.RenderedContent rendered = articleContentService.render(markdown, ArticleContentFormat.MARKDOWN);

        // Then: 렌더링된 HTML과 원본 Markdown이 함께 유지되어야 한다.
        assertThat(rendered.contentSource()).isEqualTo(markdown);
        assertThat(rendered.content()).contains("<h1>제목</h1>");
        assertThat(rendered.content()).contains("<table>");
    }

    // Markdown frontmatter는 contentSource에 보존하되 렌더 HTML에는 포함하지 않아야 한다.
    @Test
    void render_markdown_strips_frontmatter_before_rendering() {
        // Given: frontmatter가 포함된 Markdown 원본
        ArticleContentService articleContentService = new ArticleContentService(createSanitizer());
        String markdown = """
            ---
            title: "문서 제목"
            visibility: "PUBLIC"
            ---

            # 본문
            """;

        // When: Markdown을 HTML로 렌더링하면
        ArticleContentService.RenderedContent rendered = articleContentService.render(markdown, ArticleContentFormat.MARKDOWN);

        // Then: 원본은 그대로 유지하고 HTML에는 frontmatter가 포함되지 않아야 한다.
        assertThat(rendered.contentSource()).isEqualTo(markdown);
        assertThat(rendered.content()).contains("<h1>본문</h1>");
        assertThat(rendered.content()).doesNotContain("문서 제목");
        assertThat(rendered.content()).doesNotContain("visibility:");
    }

    // 표 앞뒤 공백 줄이 없어도 Markdown 표를 HTML table로 렌더링해야 한다.
    @Test
    void render_markdown_table_without_blank_lines_renders_table() {
        // Given: 표 앞에 빈 줄이 없는 Markdown 원본
        ArticleContentService articleContentService = new ArticleContentService(createSanitizer());
        String markdown = """
            소개 문단
            | 항목 | 값 |
            | --- | --- |
            | a | b |
            다음 문단
            """;

        // When: Markdown을 HTML로 렌더링하면
        ArticleContentService.RenderedContent rendered = articleContentService.render(markdown, ArticleContentFormat.MARKDOWN);

        // Then: 원본은 그대로 유지되고 표는 HTML table로 렌더링되어야 한다.
        assertThat(rendered.contentSource()).isEqualTo(markdown);
        assertThat(rendered.content()).contains("<p>소개 문단</p>");
        assertThat(rendered.content()).contains("<table>");
        assertThat(rendered.content()).contains("<td>a</td>");
        assertThat(rendered.content()).contains("<p>다음 문단</p>");
    }

    // HTML 원본은 sanitize 결과를 content와 contentSource에 동일하게 반영해야 한다.
    @Test
    void render_html_returns_sanitized_html_for_content_and_source() {
        // Given: HTML 원본과 렌더 서비스
        ArticleContentService articleContentService = new ArticleContentService(createSanitizer());
        String html = "<script>alert('x')</script><p>안전</p>";

        // When: HTML 원본을 렌더링하면
        ArticleContentService.RenderedContent rendered = articleContentService.render(html, ArticleContentFormat.HTML);

        // Then: sanitize된 HTML만 저장 대상이 되어야 한다.
        assertThat(rendered.content()).doesNotContain("<script>");
        assertThat(rendered.contentSource()).isEqualTo(rendered.content());
        assertThat(rendered.content()).contains("<p>안전</p>");
    }

    private HtmlSanitizer createSanitizer() {
        HtmlSanitizerProperties properties = new HtmlSanitizerProperties();
        HtmlSanitizer sanitizer = new HtmlSanitizer(properties);
        sanitizer.initialize();
        return sanitizer;
    }
}
