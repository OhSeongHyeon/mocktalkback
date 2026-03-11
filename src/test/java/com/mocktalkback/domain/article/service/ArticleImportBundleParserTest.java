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
        assertThat(article.markdownPath()).isEqualTo("batch/posts/post-1.md");
        assertThat(article.title()).isEqualTo("frontmatter 제목");
        assertThat(article.boardSlug()).isEqualTo("dev");
        assertThat(article.visibility()).isEqualTo("PUBLIC");
        assertThat(article.categoryName()).isEqualTo("manifest 카테고리");
        assertThat(article.contentSource()).contains("title: \"frontmatter 제목\"");
        assertThat(article.contentSource()).contains("# 본문");
        assertThat(article.warnings()).contains("frontmatter tags는 원본 content_source에 보존되며 별도 UI에는 아직 반영되지 않습니다.");
        assertThat(article.warnings()).contains("frontmatter summary는 원본 content_source에 보존되며 별도 UI에는 아직 반영되지 않습니다.");
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
        assertThat(article.warnings()).contains("frontmatter 종료 구분자가 없어 원본 전체를 Markdown으로 사용합니다.");
    }

    // manifest가 없어도 zip 안의 markdown 파일을 자동 스캔해 후보를 만들어야 한다.
    @Test
    void parse_bundle_scans_markdown_files_without_manifest() throws Exception {
        // Given: manifest 없이 여러 markdown과 assets가 포함된 zip
        ArticleImportBundleParser parser = new ArticleImportBundleParser();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "batch.zip",
            "application/zip",
            createZip(
                "docs/post-1.md", """
                    ---
                    title: "첫 번째 문서"
                    boardSlug: "dev"
                    ---

                    # 본문 1
                    """,
                "post-2.md", """
                    # 본문 2
                    """,
                "assets/cover.png", "png"
            )
        );

        // When: zip을 파싱하면
        ArticleImportBundleParser.ArticleImportBundle bundle = parser.parse(file);

        // Then: markdown 파일 2개가 자동 스캔되어야 한다.
        assertThat(bundle.articles()).hasSize(2);
        assertThat(bundle.articles())
            .extracting(ArticleImportBundleParser.ArticleImportCandidate::markdownPath)
            .containsExactly("docs/post-1.md", "post-2.md");
        assertThat(bundle.articles().get(0).title()).isEqualTo("첫 번째 문서");
        assertThat(bundle.articles().get(1).title()).isEqualTo("post 2");
    }

    private byte[] createZip(String... entries) throws Exception {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("zip 엔트리는 path/content 쌍이어야 합니다.");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int index = 0; index < entries.length; index += 2) {
                zipOutputStream.putNextEntry(new ZipEntry(entries[index]));
                zipOutputStream.write(entries[index + 1].getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
