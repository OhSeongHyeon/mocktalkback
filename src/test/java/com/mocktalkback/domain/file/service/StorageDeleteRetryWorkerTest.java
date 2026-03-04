package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mocktalkback.domain.file.repository.FileRepository;

@ExtendWith(MockitoExtension.class)
class StorageDeleteRetryWorkerTest {

    @Mock
    private StorageDeleteRetryQueueStore storageDeleteRetryQueueStore;

    @Mock
    private StorageDeleteRetryService storageDeleteRetryService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorage fileStorage;

    private StorageDeleteRetryWorker storageDeleteRetryWorker;
    private StorageDeleteRetryProperties storageDeleteRetryProperties;

    @BeforeEach
    void setUp() {
        storageDeleteRetryProperties = new StorageDeleteRetryProperties();
        storageDeleteRetryProperties.setEnabled(true);
        storageDeleteRetryProperties.setBatchSize(50);
        storageDeleteRetryProperties.setMaxAttempts(3);
        storageDeleteRetryProperties.setDlqRetentionSeconds(600L);

        storageDeleteRetryWorker = new StorageDeleteRetryWorker(
            storageDeleteRetryQueueStore,
            storageDeleteRetryService,
            storageDeleteRetryProperties,
            fileRepository,
            fileStorage
        );
    }

    // 재시도 삭제가 성공하면 큐에서 작업이 제거되어야 한다.
    @Test
    void processRetryQueue_when_delete_success_deletes_retry_job() {
        // given: 처리 대상 재시도 작업과 활성 참조 없음
        StorageDeleteRetryJob job = new StorageDeleteRetryJob(
            "job-1",
            "uploads/test-1.png",
            StorageDeleteSource.UPLOAD_CANCEL,
            "ctx-1",
            1,
            "error",
            1L,
            1L,
            1L
        );
        when(storageDeleteRetryQueueStore.popDueRetryJobs(any(Long.class), eq(50))).thenReturn(List.of(job));
        when(fileRepository.existsByStorageKeyAndDeletedAtIsNull(eq(job.storageKey()))).thenReturn(false);

        // when: 재시도 워커를 실행하면
        storageDeleteRetryWorker.processRetryQueue();

        // then: 삭제 성공 후 작업이 큐에서 제거된다.
        verify(fileStorage).delete(eq(job.storageKey()));
        verify(storageDeleteRetryQueueStore).deleteRetryJob(eq(job.jobId()));
        verify(storageDeleteRetryService, never()).enqueueRetry(any(), any(), any(), any(Integer.class), any());
    }

    // 활성 파일 참조가 있으면 삭제를 시도하지 않고 큐에서 제거해야 한다.
    @Test
    void processRetryQueue_when_active_reference_exists_skips_delete() {
        // given: 활성 참조가 남아 있는 재시도 작업
        StorageDeleteRetryJob job = new StorageDeleteRetryJob(
            "job-2",
            "uploads/test-2.png",
            StorageDeleteSource.UPLOAD_ORPHAN_CLEANUP,
            "ctx-2",
            1,
            "error",
            1L,
            1L,
            1L
        );
        when(storageDeleteRetryQueueStore.popDueRetryJobs(any(Long.class), eq(50))).thenReturn(List.of(job));
        when(fileRepository.existsByStorageKeyAndDeletedAtIsNull(eq(job.storageKey()))).thenReturn(true);

        // when: 재시도 워커를 실행하면
        storageDeleteRetryWorker.processRetryQueue();

        // then: 삭제 호출 없이 큐에서만 제거된다.
        verify(fileStorage, never()).delete(any());
        verify(storageDeleteRetryQueueStore).deleteRetryJob(eq(job.jobId()));
    }

    // 재시도 삭제가 실패하고 최대 횟수 미만이면 다시 큐에 적재해야 한다.
    @Test
    void processRetryQueue_when_delete_fails_before_max_reenqueues_retry() {
        // given: 재시도 삭제가 실패하는 작업(현재 2회)
        StorageDeleteRetryJob job = new StorageDeleteRetryJob(
            "job-3",
            "uploads/test-3.png",
            StorageDeleteSource.TEMP_FILE_CLEANUP,
            "ctx-3",
            2,
            "error",
            1L,
            1L,
            1L
        );
        when(storageDeleteRetryQueueStore.popDueRetryJobs(any(Long.class), eq(50))).thenReturn(List.of(job));
        when(fileRepository.existsByStorageKeyAndDeletedAtIsNull(eq(job.storageKey()))).thenReturn(false);
        doThrow(new IllegalStateException("delete failed")).when(fileStorage).delete(eq(job.storageKey()));

        // when: 재시도 워커를 실행하면
        storageDeleteRetryWorker.processRetryQueue();

        // then: 다음 시도(3회)로 재적재 요청된다.
        verify(storageDeleteRetryService).enqueueRetry(
            eq(job.storageKey()),
            eq(job.source()),
            eq(job.contextId()),
            eq(3),
            any(Exception.class)
        );
        verify(storageDeleteRetryQueueStore, never()).moveToDlq(any(), any(Long.class), any(Long.class));
    }

    // 재시도 삭제가 최대 횟수를 넘기면 DLQ로 이관해야 한다.
    @Test
    void processRetryQueue_when_delete_fails_over_max_moves_to_dlq() {
        // given: 최대 횟수(3회) 상태에서 다시 실패하는 작업
        StorageDeleteRetryJob job = new StorageDeleteRetryJob(
            "job-4",
            "uploads/test-4.png",
            StorageDeleteSource.UPLOAD_COMPLETE_ROLLBACK,
            "ctx-4",
            3,
            "error",
            1L,
            1L,
            1L
        );
        when(storageDeleteRetryQueueStore.popDueRetryJobs(any(Long.class), eq(50))).thenReturn(List.of(job));
        when(fileRepository.existsByStorageKeyAndDeletedAtIsNull(eq(job.storageKey()))).thenReturn(false);
        doThrow(new IllegalStateException("delete failed")).when(fileStorage).delete(eq(job.storageKey()));

        // when: 재시도 워커를 실행하면
        storageDeleteRetryWorker.processRetryQueue();

        // then: 다음 시도(4회) 기록으로 DLQ 이관된다.
        ArgumentCaptor<StorageDeleteRetryJob> captor = ArgumentCaptor.forClass(StorageDeleteRetryJob.class);
        verify(storageDeleteRetryQueueStore).moveToDlq(
            captor.capture(),
            any(Long.class),
            eq(storageDeleteRetryProperties.resolveDlqRetentionSeconds())
        );
        verify(storageDeleteRetryService, never()).enqueueRetry(any(), any(), any(), anyInt(), any());
        StorageDeleteRetryJob dlqJob = captor.getValue();
        assertThat(dlqJob.attempt()).isEqualTo(4);
    }

    // 워커가 비활성화되면 재시도 큐를 처리하지 않아야 한다.
    @Test
    void processRetryQueue_when_disabled_skips_processing() {
        // given: 워커 비활성 설정
        storageDeleteRetryProperties.setEnabled(false);

        // when: 재시도 워커를 실행하면
        storageDeleteRetryWorker.processRetryQueue();

        // then: 큐 조회를 수행하지 않는다.
        verify(storageDeleteRetryQueueStore, never()).popDueRetryJobs(any(Long.class), anyInt());
    }
}
