package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ArticleImportBundleParserTest {

    // manifest와 frontmatter가 함께 있을 때 우선순위에 맞게 메타와 본문을 추출해야 한다.
    @Test
    void parse_bundle_merges_manifest_and_frontmatter() throws Exception {
        // Given: manifest와 markdown 파일이 포함된 zip
        ArticleImportBundleParser parser = new ArticleImportBundleParser();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "batch.zip",
            "application/zip",
            createZip(
                "batch/manifest.yml", """
                    version: 1
                    defaults:
                      boardSlug: dev
                      visibility: MEMBERS
                      categoryName: "기본 카테고리"
                    articles:
                      - file: posts/post-1.md
                        visibility: PUBLIC
                        categoryName: "manifest 카테고리"
                    """,
                "batch/posts/post-1.md", """
                    ---
                    title: "frontmatter 제목"
                    categoryName: "frontmatter 카테고리"
                    tags:
                      - markdown
                    summary: "요약"
                    ---

                    # 본문
                    """
            )
        );

        // When: zip을 파싱하면
        ArticleImportBundleParser.ArticleImportBundle bundle = parser.parse(file);

        // Then: 제목/게시판/공개범위/본문이 기대대로 추출되어야 한다.
        assertThat(bundle.articles()).hasSize(1);
        ArticleImportBundleParser.ArticleImportCandidate article = bundle.articles().get(0);
        assertThat(article.filePath()).isEqualTo("posts/post-1.md");
        assertThat(article.title()).isEqualTo("frontmatter 제목");
        assertThat(article.boardSlug()).isEqualTo("dev");
        assertThat(article.visibility()).isEqualTo("PUBLIC");
        assertThat(article.categoryName()).isEqualTo("manifest 카테고리");
        assertThat(article.contentSource()).doesNotContain("title:");
        assertThat(article.contentSource()).contains("# 본문");
        assertThat(article.warnings()).contains("frontmatter tags는 아직 자동 반영되지 않아 무시됩니다.");
        assertThat(article.warnings()).contains("frontmatter summary는 아직 자동 반영되지 않아 무시됩니다.");
    }

    // frontmatter 종료 구분자가 없으면 본문 전체를 Markdown으로 사용하고 경고를 남겨야 한다.
    @Test
    void parse_bundle_keeps_markdown_when_frontmatter_is_not_closed() throws Exception {
        // Given: 닫히지 않은 frontmatter가 포함된 zip
        ArticleImportBundleParser parser = new ArticleImportBundleParser();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "batch.zip",
            "application/zip",
            createZip(
                "manifest.yml", """
                    version: 1
                    defaults:
                      boardSlug: dev
                    articles:
                      - file: post.md
                    """,
                "post.md", """
                    ---
                    title: "닫히지 않음"
                    # 본문
                    """
            )
        );

        // When: zip을 파싱하면
        ArticleImportBundleParser.ArticleImportBundle bundle = parser.parse(file);

        // Then: 본문 전체를 유지하고 경고를 반환해야 한다.
        ArticleImportBundleParser.ArticleImportCandidate article = bundle.articles().get(0);
        assertThat(article.title()).isEqualTo("post");
        assertThat(article.contentSource()).contains("title: \"닫히지 않음\"");
        assertThat(article.warnings()).contains("frontmatter 종료 구분자가 없어 본문 전체를 Markdown으로 사용합니다.");
    }

    private byte[] createZip(String firstPath, String firstContent, String secondPath, String secondContent) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry(firstPath));
            zipOutputStream.write(firstContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry(secondPath));
            zipOutputStream.write(secondContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }
}
