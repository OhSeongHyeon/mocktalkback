package com.mocktalkback.domain.file.upload.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.service.StorageDeleteRetryService;
import com.mocktalkback.domain.file.service.StorageDeleteSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UploadOrphanCleanupService {

    private final UploadOrphanTrackerRedisStore uploadOrphanTrackerRedisStore;
    private final UploadSessionRedisStore uploadSessionRedisStore;
    private final FileRepository fileRepository;
    private final StorageDeleteRetryService storageDeleteRetryService;
    private final int batchSize;

    public UploadOrphanCleanupService(
        UploadOrphanTrackerRedisStore uploadOrphanTrackerRedisStore,
        UploadSessionRedisStore uploadSessionRedisStore,
        FileRepository fileRepository,
        StorageDeleteRetryService storageDeleteRetryService,
        @Value("${app.upload.orphan-cleanup-batch-size:200}") int batchSize
    ) {
        this.uploadOrphanTrackerRedisStore = uploadOrphanTrackerRedisStore;
        this.uploadSessionRedisStore = uploadSessionRedisStore;
        this.fileRepository = fileRepository;
        this.storageDeleteRetryService = storageDeleteRetryService;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${app.upload.orphan-cleanup-interval-ms:300000}")
    @Transactional
    public void cleanupOrphanObjects() {
        long nowEpochSeconds = Instant.now().getEpochSecond();
        List<String> expiredTokens = uploadOrphanTrackerRedisStore.popExpiredTokens(nowEpochSeconds, batchSize);
        for (String token : expiredTokens) {
            cleanupToken(token);
        }
    }

    private void cleanupToken(String uploadToken) {
        if (!StringUtils.hasText(uploadToken)) {
            return;
        }
        try {
            if (uploadSessionRedisStore.find(uploadToken).isPresent()) {
                // 세션이 살아있으면 아직 업로드/완료 진행중이므로 메타만 정리하지 않는다.
                return;
            }

            String storageKey = uploadOrphanTrackerRedisStore.getTrackedStorageKey(uploadToken);
            if (!StringUtils.hasText(storageKey)) {
                return;
            }
            if (fileRepository.existsByStorageKeyAndDeletedAtIsNull(storageKey)) {
                return;
            }
            storageDeleteRetryService.deleteNowOrEnqueue(
                storageKey,
                StorageDeleteSource.UPLOAD_ORPHAN_CLEANUP,
                uploadToken
            );
            log.info("Presigned 업로드 고아 파일 정리 완료: token={}", uploadToken);
        } catch (Exception ex) {
            log.warn("Presigned 업로드 고아 파일 정리 실패: token={}", uploadToken, ex);
        } finally {
            uploadOrphanTrackerRedisStore.deleteTokenMeta(uploadToken);
        }
    }
}
