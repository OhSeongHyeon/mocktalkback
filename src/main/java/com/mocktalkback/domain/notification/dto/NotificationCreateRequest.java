package com.mocktalkback.domain.notification.dto;

import com.mocktalkback.domain.notification.type.NotificationType;
import com.mocktalkback.domain.notification.type.ReferenceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Notification create request")
public record NotificationCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Sender id", example = "2")
    @Positive
    Long senderId,

    @Schema(description = "Notification type", example = "ARTICLE_COMMENT")
    @NotNull
    NotificationType notiType,

    @Schema(description = "Redirect url", example = "/boards/1/articles/1")
    @Size(max = 1024)
    String redirectUrl,

    @Schema(description = "Reference type", example = "ARTICLE")
    @NotNull
    ReferenceType referenceType,

    @Schema(description = "Reference id", example = "1")
    @NotNull
    @Positive
    Long referenceId,

    @Schema(description = "Read flag", example = "false")
    boolean read
) {
}
