package com.mocktalkback.domain.file.service;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mocktalkback.domain.file.repository.FileRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StorageDeleteRetryWorker {

    private final StorageDeleteRetryQueueStore storageDeleteRetryQueueStore;
    private final StorageDeleteRetryService storageDeleteRetryService;
    private final StorageDeleteRetryProperties storageDeleteRetryProperties;
    private final FileRepository fileRepository;
    private final FileStorage fileStorage;

    public StorageDeleteRetryWorker(
        StorageDeleteRetryQueueStore storageDeleteRetryQueueStore,
        StorageDeleteRetryService storageDeleteRetryService,
        StorageDeleteRetryProperties storageDeleteRetryProperties,
        FileRepository fileRepository,
        FileStorage fileStorage
    ) {
        this.storageDeleteRetryQueueStore = storageDeleteRetryQueueStore;
        this.storageDeleteRetryService = storageDeleteRetryService;
        this.storageDeleteRetryProperties = storageDeleteRetryProperties;
        this.fileRepository = fileRepository;
        this.fileStorage = fileStorage;
    }

    @Scheduled(fixedDelayString = "${app.file.delete-retry.interval-ms:30000}")
    public void processRetryQueue() {
        if (!storageDeleteRetryProperties.isEnabled()) {
            return;
        }
        long nowEpochSeconds = Instant.now().getEpochSecond();
        List<StorageDeleteRetryJob> dueJobs = storageDeleteRetryQueueStore.popDueRetryJobs(
            nowEpochSeconds,
            storageDeleteRetryProperties.resolveBatchSize()
        );
        for (StorageDeleteRetryJob job : dueJobs) {
            processJob(job, nowEpochSeconds);
        }
        pruneDlq(nowEpochSeconds);
    }

    private void processJob(StorageDeleteRetryJob job, long nowEpochSeconds) {
        if (job == null) {
            return;
        }
        if (fileRepository.existsByStorageKeyAndDeletedAtIsNull(job.storageKey())) {
            // 이미 활성 파일로 참조되면 삭제하면 안 되므로 큐에서 제거한다.
            storageDeleteRetryQueueStore.deleteRetryJob(job.jobId());
            log.info(
                "삭제 재시도 작업 스킵(활성 참조 존재): source={}, storageKey={}, contextId={}",
                job.source(),
                job.storageKey(),
                job.contextId()
            );
            return;
        }

        try {
            fileStorage.delete(job.storageKey());
            storageDeleteRetryQueueStore.deleteRetryJob(job.jobId());
            log.info(
                "오브젝트 삭제 재시도 성공: source={}, storageKey={}, attempt={}, contextId={}",
                job.source(),
                job.storageKey(),
                job.attempt(),
                job.contextId()
            );
        } catch (Exception ex) {
            handleFailure(job, nowEpochSeconds, ex);
        }
    }

    private void handleFailure(StorageDeleteRetryJob job, long nowEpochSeconds, Exception ex) {
        int nextAttempt = job.attempt() + 1;
        String errorMessage = ex.getMessage();
        if (nextAttempt > storageDeleteRetryProperties.resolveMaxAttempts()) {
            StorageDeleteRetryJob dlqJob = new StorageDeleteRetryJob(
                job.jobId(),
                job.storageKey(),
                job.source(),
                job.contextId(),
                nextAttempt,
                errorMessage,
                job.createdAtEpochSeconds(),
                nowEpochSeconds,
                job.nextRetryAtEpochSeconds()
            );
            storageDeleteRetryQueueStore.moveToDlq(
                dlqJob,
                nowEpochSeconds,
                storageDeleteRetryProperties.resolveDlqRetentionSeconds()
            );
            log.error(
                "오브젝트 삭제 재시도 최대 횟수 초과로 DLQ 이관: source={}, storageKey={}, attempt={}, contextId={}",
                job.source(),
                job.storageKey(),
                nextAttempt,
                job.contextId(),
                ex
            );
            return;
        }

        storageDeleteRetryService.enqueueRetry(
            job.storageKey(),
            job.source(),
            job.contextId(),
            nextAttempt,
            ex
        );
    }

    private void pruneDlq(long nowEpochSeconds) {
        long retentionSeconds = storageDeleteRetryProperties.resolveDlqRetentionSeconds();
        if (retentionSeconds <= 0L) {
            return;
        }
        long pruneBeforeEpochSeconds = nowEpochSeconds - retentionSeconds;
        if (pruneBeforeEpochSeconds <= 0L) {
            return;
        }
        storageDeleteRetryQueueStore.pruneDlq(
            pruneBeforeEpochSeconds,
            storageDeleteRetryProperties.resolveBatchSize()
        );
    }
}

