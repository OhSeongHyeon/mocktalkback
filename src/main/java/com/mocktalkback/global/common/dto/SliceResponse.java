package com.mocktalkback.global.common.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "슬라이스 응답")
public record SliceResponse<T>(
    @Schema(description = "목록")
    List<T> items,

    @Schema(description = "페이지 번호(0부터 시작)", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext,

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    boolean hasPrevious
) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(
            slice.getContent(),
            slice.getNumber(),
            slice.getSize(),
            slice.hasNext(),
            slice.hasPrevious()
        );
    }
}
