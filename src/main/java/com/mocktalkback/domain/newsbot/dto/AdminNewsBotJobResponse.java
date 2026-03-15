package com.mocktalkback.domain.newsbot.dto;

import java.time.Instant;
import java.util.Map;

import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;
import com.mocktalkback.domain.newsbot.type.NewsSourceType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "뉴스봇 잡 응답")
public record AdminNewsBotJobResponse(
    @Schema(description = "잡 ID", example = "1")
    Long jobId,

    @Schema(description = "잡 이름", example = "백엔드 새소식")
    String jobName,

    @Schema(description = "외부 소스 유형", example = "DEV_TO")
    NewsSourceType sourceType,

    @Schema(description = "외부 소스 설정")
    Map<String, Object> sourceConfig,

    @Schema(description = "대상 게시판 slug", example = "backend-news")
    String targetBoardSlug,

    @Schema(description = "자동 생성 시 게시판 이름", example = "백엔드 새소식")
    String targetBoardName,

    @Schema(description = "기본 카테고리명", example = "DEV")
    String targetCategoryName,

    @Schema(description = "작성자 user_id", example = "2")
    Long authorUserId,

    @Schema(description = "작성자 표시명", example = "뉴스봇")
    String authorDisplayName,

    @Schema(description = "활성화 여부", example = "true")
    boolean enabled,

    @Schema(description = "수집 주기(분)", example = "60")
    int collectIntervalMinutes,

    @Schema(description = "1회 수집 최대 건수", example = "20")
    int fetchLimit,

    @Schema(description = "게시판 자동 생성 허용 여부", example = "false")
    boolean autoCreateBoard,

    @Schema(description = "카테고리 자동 생성 허용 여부", example = "true")
    boolean autoCreateCategory,

    @Schema(description = "잡 시간대", example = "Asia/Seoul")
    String timezone,

    @Schema(description = "마지막 시작 시각")
    Instant lastStartedAt,

    @Schema(description = "마지막 종료 시각")
    Instant lastFinishedAt,

    @Schema(description = "마지막 성공 시각")
    Instant lastSuccessAt,

    @Schema(description = "다음 실행 시각")
    Instant nextRunAt,

    @Schema(description = "마지막 상태", example = "SUCCESS")
    NewsJobExecutionStatus lastStatus,

    @Schema(description = "마지막 오류 메시지")
    String lastErrorMessage,

    @Schema(description = "생성 시각")
    Instant createdAt,

    @Schema(description = "수정 시각")
    Instant updatedAt
) {
}
