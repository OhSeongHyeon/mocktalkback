package com.mocktalkback.domain.article.dto;

import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.file.dto.FileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article board summary response")
public record ArticleBoardResponse(
    @Schema(description = "Board id", example = "1")
    Long id,

    @Schema(description = "Board name", example = "Notice")
    String boardName,

    @Schema(description = "Board slug", example = "notice")
    String slug,

    @Schema(description = "Board description", example = "Board for announcements")
    String description,

    @Schema(description = "Board visibility", example = "PUBLIC")
    BoardVisibility visibility,

    @Schema(description = "Board image")
    FileResponse boardImage
) {
}
