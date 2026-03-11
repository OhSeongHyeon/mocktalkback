package com.mocktalkback.domain.article.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 대량 임포트 미리보기 응답")
public record ArticleImportPreviewResponse(
    @Schema(description = "실행 가능한 항목이 하나 이상 있는지 여부", example = "true")
    boolean canExecute,

    @Schema(description = "전체 항목 수", example = "3")
    int totalCount,

    @Schema(description = "실행 가능한 항목 수", example = "2")
    int executableCount,

    @Schema(description = "실행 불가능한 항목 수", example = "1")
    int invalidCount,

    @Schema(description = "문서별 미리보기 항목")
    List<ArticleImportPreviewItemResponse> items
) {
}
