package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

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
class ArticleAttachmentFileServiceTest {

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

    // 게시글 첨부파일 업로드는 ARTICLE_ATTACHMENT 클래스로 저장되고 임시 만료를 설정해야 한다.
    @Test
    void uploadArticleAttachmentFile_saves_article_attachment_file() {
        // Given: 업로드할 텍스트 파일과 저장 결과
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "guide.txt",
            "text/plain",
            "hello attachment".getBytes(StandardCharsets.UTF_8)
        );

        when(currentUserService.getUserId()).thenReturn(1L);

        StoredFile storedFile = new StoredFile(
            "guide.txt",
            "/uploads/1/guide.txt",
            16L,
            "text/plain"
        );
        when(fileStorage.store(eq(FileClassCode.ARTICLE_ATTACHMENT), eq(multipartFile), eq(1L)))
            .thenReturn(storedFile);

        ImageOptimizationService.OriginalFileResult processed =
            new ImageOptimizationService.OriginalFileResult(16L, "text/plain", false);
        when(imageOptimizationService.processOriginal(storedFile, true)).thenReturn(processed);

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_ATTACHMENT)
            .name("게시글 첨부파일")
            .description("게시글에 첨부되는 일반 파일")
            .mediaKind(MediaKind.ANY)
            .build();
        when(fileClassRepository.findByCode(FileClassCode.ARTICLE_ATTACHMENT))
            .thenReturn(Optional.of(fileClass));

        Instant expiresAt = Instant.parse("2026-02-27T00:00:00Z");
        when(temporaryFilePolicy.resolveExpiry()).thenReturn(expiresAt);

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 30L);
            return entity;
        });

        FileResponse fileResponse = new FileResponse(
            30L,
            null,
            "guide.txt",
            "/uploads/1/guide.txt",
            16L,
            "text/plain",
            null,
            null,
            null
        );
        when(fileMapper.toResponse(any(FileEntity.class))).thenReturn(fileResponse);

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When: 게시글 첨부파일 업로드를 호출하면
        FileResponse result = service.uploadArticleAttachmentFile(multipartFile, false);

        // Then: ARTICLE_ATTACHMENT로 저장되고 임시 만료가 설정된다.
        assertThat(result).isEqualTo(fileResponse);
        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());
        FileEntity captured = captor.getValue();
        assertThat(captured.getFileClass()).isEqualTo(fileClass);
        assertThat(captured.getFileName()).isEqualTo("guide.txt");
        assertThat(captured.getStorageKey()).isEqualTo("/uploads/1/guide.txt");
        assertThat(captured.getTempExpiresAt()).isEqualTo(expiresAt);
        assertThat(captured.isTemporary()).isTrue();
        verify(imageOptimizationService).enqueueVariantGeneration(any(FileEntity.class));
    }

    // 실행 파일 확장자는 게시글 첨부파일 업로드에서 차단되어야 한다.
    @Test
    void uploadArticleAttachmentFile_rejects_executable_extension() {
        // Given: 실행 파일 확장자 업로드 요청
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "run.exe",
            "application/octet-stream",
            "binary".getBytes(StandardCharsets.UTF_8)
        );

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When & Then: 업로드 호출 시 예외가 발생한다.
        assertThatThrownBy(() -> service.uploadArticleAttachmentFile(multipartFile, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("업로드할 수 없는 확장자입니다.");
    }

    // 허용되지 않은 확장자는 업로드가 거부되어야 한다.
    @Test
    void uploadArticleAttachmentFile_rejects_not_allowed_extension() {
        // Given: 허용 목록에 없는 확장자 업로드 요청
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "index.html",
            "text/html",
            "<html></html>".getBytes(StandardCharsets.UTF_8)
        );

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When & Then: 업로드 호출 시 예외가 발생한다.
        assertThatThrownBy(() -> service.uploadArticleAttachmentFile(multipartFile, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("허용되지 않는 파일 형식입니다.");
    }

    // 허용된 확장자라도 MIME 타입이 허용 목록에 없으면 업로드가 거부되어야 한다.
    @Test
    void uploadArticleAttachmentFile_rejects_not_allowed_mime_type() {
        // Given: 허용 확장자(pdf)지만 허용되지 않은 MIME 타입
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "guide.pdf",
            "text/html",
            "<html></html>".getBytes(StandardCharsets.UTF_8)
        );

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When & Then: 업로드 호출 시 예외가 발생한다.
        assertThatThrownBy(() -> service.uploadArticleAttachmentFile(multipartFile, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("허용되지 않는 파일 형식입니다.");
    }

    // application/octet-stream 은 허용 확장자일 때 업로드를 통과해야 한다.
    @Test
    void uploadArticleAttachmentFile_allows_octet_stream_for_allowed_extension() {
        // Given: 허용 확장자(pdf) + octet-stream 업로드 요청
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "guide.pdf",
            "application/octet-stream",
            "pdf-binary".getBytes(StandardCharsets.UTF_8)
        );

        when(currentUserService.getUserId()).thenReturn(1L);

        StoredFile storedFile = new StoredFile(
            "guide.pdf",
            "/uploads/1/guide.pdf",
            10L,
            "application/octet-stream"
        );
        when(fileStorage.store(eq(FileClassCode.ARTICLE_ATTACHMENT), eq(multipartFile), eq(1L)))
            .thenReturn(storedFile);

        ImageOptimizationService.OriginalFileResult processed =
            new ImageOptimizationService.OriginalFileResult(10L, "application/octet-stream", false);
        when(imageOptimizationService.processOriginal(storedFile, true)).thenReturn(processed);

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_ATTACHMENT)
            .name("게시글 첨부파일")
            .description("게시글에 첨부되는 일반 파일")
            .mediaKind(MediaKind.ANY)
            .build();
        when(fileClassRepository.findByCode(FileClassCode.ARTICLE_ATTACHMENT))
            .thenReturn(Optional.of(fileClass));

        Instant expiresAt = Instant.parse("2026-02-27T00:00:00Z");
        when(temporaryFilePolicy.resolveExpiry()).thenReturn(expiresAt);

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 31L);
            return entity;
        });

        FileResponse fileResponse = new FileResponse(
            31L,
            null,
            "guide.pdf",
            "/uploads/1/guide.pdf",
            10L,
            "application/octet-stream",
            null,
            null,
            null
        );
        when(fileMapper.toResponse(any(FileEntity.class))).thenReturn(fileResponse);

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When: 업로드를 수행하면
        FileResponse result = service.uploadArticleAttachmentFile(multipartFile, false);

        // Then: 정상 저장 응답을 반환한다.
        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.fileName()).isEqualTo("guide.pdf");
    }

    // 오디오 첨부(mp3)는 허용 목록에 포함되어 업로드가 가능해야 한다.
    @Test
    void uploadArticleAttachmentFile_allows_mp3_file() {
        // Given: 허용 오디오 확장자(mp3) 업로드 요청
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "sample.mp3",
            "audio/mpeg",
            "audio-binary".getBytes(StandardCharsets.UTF_8)
        );

        when(currentUserService.getUserId()).thenReturn(1L);

        StoredFile storedFile = new StoredFile(
            "sample.mp3",
            "/uploads/1/sample.mp3",
            12L,
            "audio/mpeg"
        );
        when(fileStorage.store(eq(FileClassCode.ARTICLE_ATTACHMENT), eq(multipartFile), eq(1L)))
            .thenReturn(storedFile);

        ImageOptimizationService.OriginalFileResult processed =
            new ImageOptimizationService.OriginalFileResult(12L, "audio/mpeg", false);
        when(imageOptimizationService.processOriginal(storedFile, true)).thenReturn(processed);

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_ATTACHMENT)
            .name("게시글 첨부파일")
            .description("게시글에 첨부되는 일반 파일")
            .mediaKind(MediaKind.ANY)
            .build();
        when(fileClassRepository.findByCode(FileClassCode.ARTICLE_ATTACHMENT))
            .thenReturn(Optional.of(fileClass));

        Instant expiresAt = Instant.parse("2026-02-27T00:00:00Z");
        when(temporaryFilePolicy.resolveExpiry()).thenReturn(expiresAt);

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 32L);
            return entity;
        });

        FileResponse fileResponse = new FileResponse(
            32L,
            null,
            "sample.mp3",
            "/uploads/1/sample.mp3",
            12L,
            "audio/mpeg",
            null,
            null,
            null
        );
        when(fileMapper.toResponse(any(FileEntity.class))).thenReturn(fileResponse);

        ArticleAttachmentFileService service = new ArticleAttachmentFileService(
            fileStorage,
            fileRepository,
            fileClassRepository,
            fileMapper,
            currentUserService,
            imageOptimizationService,
            temporaryFilePolicy
        );

        // When: 업로드를 수행하면
        FileResponse result = service.uploadArticleAttachmentFile(multipartFile, false);

        // Then: 정상 저장 응답을 반환한다.
        assertThat(result.id()).isEqualTo(32L);
        assertThat(result.fileName()).isEqualTo("sample.mp3");
    }
}
