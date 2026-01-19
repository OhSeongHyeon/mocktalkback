package com.mocktalkback.domain.article.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Article bookmark delete request")
public record ArticleBookmarkDeleteRequest(
    @Schema(description = "Article ids", example = "[1, 2]")
    @NotNull
    @NotEmpty
    List<@NotNull @Positive Long> articleIds
) {
}
