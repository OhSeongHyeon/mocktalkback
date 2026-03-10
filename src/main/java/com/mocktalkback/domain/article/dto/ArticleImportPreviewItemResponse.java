package com.mocktalkback.domain.article.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 대량 임포트 미리보기 항목")
public record ArticleImportPreviewItemResponse(
    @Schema(description = "zip 내부 markdown 파일 경로", example = "posts/post-1.md")
    String filePath,

    @Schema(description = "추출된 제목", example = "Mermaid 렌더링 정리")
    String title,

    @Schema(description = "대상 게시판 slug", example = "dev")
    String boardSlug,

    @Schema(description = "공개 범위", example = "PUBLIC")
    String visibility,

    @Schema(description = "실행 가능 여부", example = "true")
    boolean executable,

    @Schema(description = "경고 목록")
    List<String> warnings,

    @Schema(description = "오류 목록")
    List<String> errors
) {
}
