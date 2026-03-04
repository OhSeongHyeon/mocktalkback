package com.mocktalkback.domain.file.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditorFileService {

    private static final long MAX_UPLOAD_SIZE = 50L * 1024L * 1024L;

    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileMapper fileMapper;
    private final ImageOptimizationService imageOptimizationService;
    private final TemporaryFilePolicy temporaryFilePolicy;

    @Transactional
    public FileResponse completeEditorFileUpload(StoredFile storedFile, boolean preserveMetadata) {
        validateStoredFile(storedFile);
        String fileClassCode = resolveFileClassCode(storedFile.mimeType());

        ImageOptimizationService.OriginalFileResult processed = imageOptimizationService
            .processOriginal(storedFile, preserveMetadata);
        FileClassEntity fileClass = getOrCreateFileClass(fileClassCode);

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClass)
            .fileName(storedFile.fileName())
            .storageKey(storedFile.storageKey())
            .fileSize(processed.fileSize())
            .mimeType(processed.mimeType())
            .metadataPreserved(processed.metadataPreserved())
            .tempExpiresAt(temporaryFilePolicy.resolveExpiry())
            .build();

        FileEntity saved = fileRepository.save(fileEntity);
        imageOptimizationService.enqueueVariantGeneration(saved);
        return fileMapper.toResponse(saved);
    }

    private void validateStoredFile(StoredFile storedFile) {
        if (storedFile == null) {
            throw new IllegalArgumentException("업로드 파일 정보가 비어있습니다.");
        }
        if (storedFile.fileSize() == null || storedFile.fileSize() <= 0L) {
            throw new IllegalArgumentException("파일 크기가 올바르지 않습니다.");
        }
        if (storedFile.fileSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("파일 사이즈 제한 50MB");
        }
        String contentType = storedFile.mimeType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
        if (contentType.startsWith("image/")) {
            return;
        }
        if ("video/mp4".equals(contentType) || "video/webm".equals(contentType)) {
            return;
        }
        throw new IllegalArgumentException("이미지 또는 MP4/WebM 영상만 업로드할 수 있습니다.");
    }

    private String resolveFileClassCode(String contentType) {
        if (StringUtils.hasText(contentType) && contentType.startsWith("image/")) {
            return FileClassCode.ARTICLE_CONTENT_IMAGE;
        }
        return FileClassCode.ARTICLE_CONTENT_VIDEO;
    }

    private FileClassEntity getOrCreateFileClass(String fileClassCode) {
        return fileClassRepository.findByCode(fileClassCode)
            .orElseGet(() -> fileClassRepository.save(createFileClass(fileClassCode)));
    }

    private FileClassEntity createFileClass(String fileClassCode) {
        if (FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)) {
            return FileClassEntity.builder()
                .code(fileClassCode)
                .name("게시글 본문 이미지")
                .description("에디터 본문에 삽입되는 이미지")
                .mediaKind(MediaKind.IMAGE)
                .build();
        }
        return FileClassEntity.builder()
            .code(fileClassCode)
            .name("게시글 본문 영상")
            .description("에디터 본문에 삽입되는 영상")
            .mediaKind(MediaKind.VIDEO)
            .build();
    }
}
