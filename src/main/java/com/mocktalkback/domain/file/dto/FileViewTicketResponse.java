package com.mocktalkback.domain.file.dto;

public record FileViewTicketResponse(
    String viewUrl,
    long expiresInSec,
    boolean protectedFile
) {
}
