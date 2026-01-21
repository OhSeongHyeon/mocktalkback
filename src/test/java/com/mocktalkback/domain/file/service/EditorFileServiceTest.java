package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class EditorFileServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileClassRepository fileClassRepository;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ImageOptimizationService imageOptimizationService;

    @Mock
    private TemporaryFilePolicy temporaryFilePolicy;

    // 에디터 파일 업로드는 임시 만료를 설정하고 변환 대기 큐를 등록해야 한다.
    @Test
    void uploadEditorFile_marks_temporary_and_enqueues_variants() throws IOException {
        // Given: 업로드 이미지와 저장 결과
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "sample.png",
            "image/png",
            createPngBytes()
        );
        when(currentUserService.getUserId()).thenReturn(1L);

        StoredFile storedFile = new StoredFile(
            "sample.png",
            "/uploads/1/sample.png",
            123L,
            "image/png"
        );
        when(fileStorage.store(eq(FileClassCode.ARTICLE_CONTENT_IMAGE), eq(multipartFile), eq(1L)))
            .thenReturn(storedFile);

        ImageOptimizationService.OriginalFileResult processed =
            new ImageOptimizationService.OriginalFileResult(123L, "image/png", false);
        when(imageOptimizationService.processOriginal(storedFile, false)).thenReturn(processed);

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_CONTENT_IMAGE)
            .name("게시글 본문 이미지")
            .description("에디터 이미지")
            .mediaKind(MediaKind.IMAGE)
            .build();
        when(fileClassRepository.findByCode(FileClassCode.ARTICLE_CONTENT_IMAGE))
            .thenReturn(Optional.of(fileClass));

        Instant expiresAt = Instant.parse("2024-01-01T00:00:00Z");
        when(temporaryFilePolicy.resolveExpiry()).thenReturn(expiresAt);

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 10L);
            return entity;
        });

        FileResponse response = new FileResponse(
            10L,
            null,
            "sample.png",
            "/uploads/1/sample.png",
            123L,
            "image/png",
            null,
            null,
            null
        );
        when(fileMapper.toResponse(any(FileEntity.class))).thenReturn(response);

        // When: 에디터 파일 업로드 호출
        FileResponse result = new EditorFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        ).uploadEditorFile(multipartFile, false);

        // Then: 임시 만료와 변환 등록 확인
        assertThat(result).isEqualTo(response);
        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());
        FileEntity captured = captor.getValue();
        assertThat(captured.getFileClass()).isEqualTo(fileClass);
        assertThat(captured.getFileName()).isEqualTo("sample.png");
        assertThat(captured.getStorageKey()).isEqualTo("/uploads/1/sample.png");
        assertThat(captured.getTempExpiresAt()).isEqualTo(expiresAt);
        assertThat(captured.isTemporary()).isTrue();
        assertThat(captured.isDeleted()).isFalse();
        verify(imageOptimizationService).enqueueVariantGeneration(any(FileEntity.class));
    }

    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
