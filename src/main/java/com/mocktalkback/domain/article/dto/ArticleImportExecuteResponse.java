package com.mocktalkback.domain.article.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 대량 임포트 실행 응답")
public record ArticleImportExecuteResponse(
    @Schema(description = "전체 항목 수", example = "3")
    int totalCount,

    @Schema(description = "생성 성공 수", example = "2")
    int successCount,

    @Schema(description = "생성 실패 수", example = "1")
    int failedCount,

    @Schema(description = "문서별 실행 결과")
    List<ArticleImportExecuteItemResponse> items
) {
}
