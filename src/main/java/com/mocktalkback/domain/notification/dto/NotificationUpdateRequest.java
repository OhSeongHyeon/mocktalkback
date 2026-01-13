package com.mocktalkback.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification update request")
public record NotificationUpdateRequest(
    @Schema(description = "Read flag", example = "true")
    boolean read
) {
}
