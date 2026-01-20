package com.mocktalkback.domain.search.dto;

import java.time.Instant;

import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.file.dto.FileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 검색 응답")
public record BoardSearchResponse(
    @Schema(description = "게시판 ID", example = "1")
    Long id,

    @Schema(description = "게시판명", example = "공지사항")
    String boardName,

    @Schema(description = "슬러그", example = "notice")
    String slug,

    @Schema(description = "설명", example = "공지사항 게시판")
    String description,

    @Schema(description = "가시성", example = "PUBLIC")
    BoardVisibility visibility,

    @Schema(description = "게시판 대표 이미지")
    FileResponse boardImage,

    @Schema(description = "생성일", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
