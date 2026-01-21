package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영 로그 응답")
public record AdminAuditLogResponse(
    @Schema(description = "로그 번호", example = "1")
    Long id,

    @Schema(description = "행위자 회원번호", example = "1")
    Long actorUserId,

    @Schema(description = "액션 유형", example = "SANCTION_CREATE")
    AdminActionType actionType,

    @Schema(description = "대상 유형", example = "SANCTION")
    AdminTargetType targetType,

    @Schema(description = "대상 식별자", example = "10")
    Long targetId,

    @Schema(description = "게시판 번호")
    Long boardId,

    @Schema(description = "요약")
    String summary,

    @Schema(description = "상세 데이터(JSON 문자열)")
    String detailJson,

    @Schema(description = "요청 IP")
    String ipAddress,

    @Schema(description = "요청 User-Agent")
    String userAgent,

    @Schema(description = "생성 일시")
    Instant createdAt
) {
}
