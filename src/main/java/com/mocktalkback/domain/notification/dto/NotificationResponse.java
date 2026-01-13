package com.mocktalkback.domain.notification.dto;

import java.time.Instant;

import com.mocktalkback.domain.notification.type.NotificationType;
import com.mocktalkback.domain.notification.type.ReferenceType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification response")
public record NotificationResponse(
    @Schema(description = "Notification id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Sender id", example = "2")
    Long senderId,

    @Schema(description = "Notification type", example = "ARTICLE_COMMENT")
    NotificationType notiType,

    @Schema(description = "Redirect url", example = "/boards/1/articles/1")
    String redirectUrl,

    @Schema(description = "Reference type", example = "ARTICLE")
    ReferenceType referenceType,

    @Schema(description = "Reference id", example = "1")
    Long referenceId,

    @Schema(description = "Read flag", example = "false")
    boolean read,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
