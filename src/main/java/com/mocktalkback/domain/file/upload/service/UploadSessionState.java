package com.mocktalkback.domain.file.upload.service;

import java.time.Instant;

import com.mocktalkback.domain.file.upload.type.BoardImageUploadChannel;
import com.mocktalkback.domain.file.upload.type.UploadPurpose;

public record UploadSessionState(
    String uploadToken,
    Long ownerId,
    UploadPurpose purpose,
    String originalFileName,
    String fileNameForDatabase,
    String expectedMimeType,
    long expectedFileSize,
    String storageKey,
    Long boardId,
    BoardImageUploadChannel boardChannel,
    boolean preserveMetadata,
    Instant createdAt
) {
}
