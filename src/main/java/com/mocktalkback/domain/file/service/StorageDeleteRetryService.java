package com.mocktalkback.domain.file.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StorageDeleteRetryService {

    private final FileStorage fileStorage;
    private final StorageDeleteRetryQueueStore storageDeleteRetryQueueStore;
    private final StorageDeleteRetryProperties storageDeleteRetryProperties;

    public StorageDeleteRetryService(
        FileStorage fileStorage,
        StorageDeleteRetryQueueStore storageDeleteRetryQueueStore,
        StorageDeleteRetryProperties storageDeleteRetryProperties
    ) {
        this.fileStorage = fileStorage;
        this.storageDeleteRetryQueueStore = storageDeleteRetryQueueStore;
        this.storageDeleteRetryProperties = storageDeleteRetryProperties;
    }

    public void deleteNowOrEnqueue(String storageKey, StorageDeleteSource source, String contextId) {
        if (!StringUtils.hasText(storageKey) || source == null) {
            return;
        }
        String normalizedStorageKey = storageKey.trim();
        try {
            fileStorage.delete(normalizedStorageKey);
        } catch (Exception ex) {
            enqueueRetry(normalizedStorageKey, source, contextId, 1, ex);
        }
    }

    public void enqueueRetry(String storageKey, StorageDeleteSource source, String contextId, int attempt, Exception cause) {
        if (!StringUtils.hasText(storageKey) || source == null) {
            return;
        }
        String normalizedStorageKey = storageKey.trim();
        String jobId = resolveJobId(normalizedStorageKey);
        long nowEpochSeconds = Instant.now().getEpochSecond();
        Optional<StorageDeleteRetryJob> existing = storageDeleteRetryQueueStore.findRetryJob(jobId);

        int resolvedAttempt = Math.max(1, attempt);
        if (existing.isPresent()) {
            resolvedAttempt = Math.max(existing.get().attempt(), resolvedAttempt);
        }
        long createdAtEpochSeconds = existing.map(StorageDeleteRetryJob::createdAtEpochSeconds).orElse(nowEpochSeconds);
        long nextRetryAtEpochSeconds = nowEpochSeconds + storageDeleteRetryProperties.resolveRetryDelaySeconds(resolvedAttempt);
        String normalizedContextId = normalizeText(contextId);
        String lastError = resolveLastError(cause);

        StorageDeleteRetryJob retryJob = new StorageDeleteRetryJob(
            jobId,
            normalizedStorageKey,
            source,
            normalizedContextId,
            resolvedAttempt,
            lastError,
            createdAtEpochSeconds,
            nowEpochSeconds,
            nextRetryAtEpochSeconds
        );

        try {
            storageDeleteRetryQueueStore.upsertRetryJob(retryJob);
            log.warn(
                "오브젝트 삭제 실패로 재시도 큐 적재: source={}, storageKey={}, attempt={}, contextId={}",
                source,
                normalizedStorageKey,
                resolvedAttempt,
                normalizedContextId
            );
        } catch (Exception queueException) {
            log.error(
                "오브젝트 삭제 재시도 큐 적재 실패: source={}, storageKey={}, attempt={}, contextId={}",
                source,
                normalizedStorageKey,
                resolvedAttempt,
                normalizedContextId,
                queueException
            );
        }
    }

    private String resolveJobId(String storageKey) {
        return DigestUtils.md5DigestAsHex(storageKey.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveLastError(Exception ex) {
        if (ex == null) {
            return "삭제 중 알 수 없는 오류";
        }
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.trim();
    }
}

