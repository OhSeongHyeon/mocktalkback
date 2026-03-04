package com.mocktalkback.domain.file.service;

public record StorageDeleteRetryJob(
    String jobId,
    String storageKey,
    StorageDeleteSource source,
    String contextId,
    int attempt,
    String lastError,
    long createdAtEpochSeconds,
    long updatedAtEpochSeconds,
    long nextRetryAtEpochSeconds
) {
}

