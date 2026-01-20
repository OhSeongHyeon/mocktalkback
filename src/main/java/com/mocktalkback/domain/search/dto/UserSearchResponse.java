package com.mocktalkback.domain.search.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 검색 응답")
public record UserSearchResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "핸들", example = "mocktalk")
    String handle,

    @Schema(description = "닉네임", example = "MockTalk")
    String displayName,

    @Schema(description = "가입일", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
