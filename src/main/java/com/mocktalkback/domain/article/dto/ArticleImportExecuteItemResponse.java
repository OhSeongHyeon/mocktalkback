package com.mocktalkback.domain.article.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 대량 임포트 실행 항목")
public record ArticleImportExecuteItemResponse(
    @Schema(description = "zip 내부 markdown 파일 경로", example = "posts/post-1.md")
    String filePath,

    @Schema(description = "추출된 제목", example = "Mermaid 렌더링 정리")
    String title,

    @Schema(description = "대상 게시판 slug", example = "dev")
    String boardSlug,

    @Schema(description = "대상 게시판 카테고리명", example = "백엔드")
    String categoryName,

    @Schema(description = "공개 범위", example = "PUBLIC")
    String visibility,

    @Schema(description = "생성 성공 여부", example = "true")
    boolean created,

    @Schema(description = "생성된 게시글 ID", example = "101")
    Long articleId,

    @Schema(description = "경고 목록")
    List<String> warnings,

    @Schema(description = "오류 목록")
    List<String> errors
) {
}
