package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageDeleteRetryServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private StorageDeleteRetryQueueStore storageDeleteRetryQueueStore;

    private StorageDeleteRetryService storageDeleteRetryService;

    @BeforeEach
    void setUp() {
        StorageDeleteRetryProperties properties = new StorageDeleteRetryProperties();
        properties.setInitialDelaySeconds(1L);
        properties.setMaxDelaySeconds(8L);
        properties.setMaxAttempts(3);

        storageDeleteRetryService = new StorageDeleteRetryService(
            fileStorage,
            storageDeleteRetryQueueStore,
            properties
        );
    }

    // 삭제가 즉시 성공하면 재시도 큐를 적재하지 않아야 한다.
    @Test
    void deleteNowOrEnqueue_when_delete_success_does_not_enqueue() {
        // given: 정상 삭제 가능한 저장소 키
        String storageKey = "uploads/article/test.png";

        // when: 즉시 삭제를 실행하면
        storageDeleteRetryService.deleteNowOrEnqueue(storageKey, StorageDeleteSource.UPLOAD_CANCEL, "token-1");

        // then: 스토리지 삭제만 호출되고 재시도 큐 적재는 발생하지 않는다.
        verify(fileStorage).delete(eq(storageKey));
        verify(storageDeleteRetryQueueStore, never()).upsertRetryJob(any());
    }

    // 삭제가 실패하면 재시도 큐에 작업을 적재해야 한다.
    @Test
    void deleteNowOrEnqueue_when_delete_fails_enqueues_retry_job() {
        // given: 삭제 실패가 발생하는 저장소 키
        String storageKey = "uploads/article/test.png";
        doThrow(new IllegalStateException("delete failed")).when(fileStorage).delete(eq(storageKey));
        when(storageDeleteRetryQueueStore.findRetryJob(any())).thenReturn(Optional.empty());

        // when: 즉시 삭제를 실행하면
        storageDeleteRetryService.deleteNowOrEnqueue(storageKey, StorageDeleteSource.UPLOAD_CANCEL, "token-2");

        // then: 재시도 큐 작업이 1회 시도로 적재된다.
        ArgumentCaptor<StorageDeleteRetryJob> captor = ArgumentCaptor.forClass(StorageDeleteRetryJob.class);
        verify(storageDeleteRetryQueueStore).upsertRetryJob(captor.capture());
        StorageDeleteRetryJob job = captor.getValue();
        assertThat(job.storageKey()).isEqualTo(storageKey);
        assertThat(job.source()).isEqualTo(StorageDeleteSource.UPLOAD_CANCEL);
        assertThat(job.contextId()).isEqualTo("token-2");
        assertThat(job.attempt()).isEqualTo(1);
        assertThat(job.lastError()).contains("delete failed");
    }
}

