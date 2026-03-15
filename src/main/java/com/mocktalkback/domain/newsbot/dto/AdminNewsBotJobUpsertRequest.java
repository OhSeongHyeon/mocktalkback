package com.mocktalkback.domain.newsbot.dto;

import java.util.Map;

import com.mocktalkback.domain.newsbot.type.NewsSourceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "뉴스봇 잡 생성/수정 요청")
public record AdminNewsBotJobUpsertRequest(
    @Schema(description = "잡 이름", example = "백엔드 새소식")
    @NotBlank
    @Size(max = 120)
    String jobName,

    @Schema(description = "외부 소스 유형", example = "DEV_TO")
    @NotNull
    NewsSourceType sourceType,

    @Schema(description = "외부 소스 설정")
    @NotNull
    @NotEmpty
    Map<String, Object> sourceConfig,

    @Schema(description = "대상 게시판 slug", example = "backend-news")
    @NotBlank
    @Size(max = 80)
    String targetBoardSlug,

    @Schema(description = "자동 생성 시 게시판 이름", example = "백엔드 새소식")
    @Size(max = 255)
    String targetBoardName,

    @Schema(description = "기본 카테고리명", example = "DEV")
    @Size(max = 48)
    String targetCategoryName,

    @Schema(description = "수집 주기(분)", example = "60")
    @Min(5)
    @Max(10080)
    int collectIntervalMinutes,

    @Schema(description = "1회 수집 최대 건수", example = "20")
    @Min(1)
    @Max(100)
    int fetchLimit,

    @Schema(description = "게시판 자동 생성 허용 여부", example = "false")
    boolean autoCreateBoard,

    @Schema(description = "카테고리 자동 생성 허용 여부", example = "true")
    boolean autoCreateCategory,

    @Schema(description = "잡 시간대", example = "Asia/Seoul")
    @Size(max = 64)
    String timezone
) {
}
