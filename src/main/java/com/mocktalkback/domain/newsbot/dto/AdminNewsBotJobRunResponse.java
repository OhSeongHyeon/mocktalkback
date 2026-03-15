package com.mocktalkback.domain.newsbot.dto;

import java.time.Instant;

import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "뉴스봇 잡 실행 결과")
public record AdminNewsBotJobRunResponse(
    @Schema(description = "잡 ID", example = "1")
    Long jobId,

    @Schema(description = "실행 시각")
    Instant executedAt,

    @Schema(description = "가져온 항목 수", example = "10")
    int fetchedCount,

    @Schema(description = "생성한 게시글 수", example = "6")
    int createdCount,

    @Schema(description = "갱신한 게시글 수", example = "2")
    int updatedCount,

    @Schema(description = "건너뛴 항목 수", example = "2")
    int skippedCount,

    @Schema(description = "실패 항목 수", example = "0")
    int failedCount,

    @Schema(description = "최종 상태", example = "SUCCESS")
    NewsJobExecutionStatus status,

    @Schema(description = "실패 메시지")
    String errorMessage
) {
}
