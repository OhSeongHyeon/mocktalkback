package com.mocktalkback.domain.board.dto;

import java.time.Instant;

import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.file.dto.FileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board subscribe item response")
public record BoardSubscribeItemResponse(
    @Schema(description = "Subscribe id", example = "1")
    Long id,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Board name", example = "Notice")
    String boardName,

    @Schema(description = "Slug", example = "notice")
    String slug,

    @Schema(description = "Description", example = "Board for announcements")
    String description,

    @Schema(description = "Visibility", example = "PUBLIC")
    BoardVisibility visibility,

    @Schema(description = "Board image")
    FileResponse boardImage,

    @Schema(description = "Subscribed at", example = "2024-01-01T00:00:00Z")
    Instant subscribedAt
) {
}
