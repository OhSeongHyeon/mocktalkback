package com.mocktalkback.domain.file.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditorFileService {

    private static final long MAX_UPLOAD_SIZE = 50L * 1024L * 1024L;

    private final FileStorage fileStorage;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileMapper fileMapper;
    private final CurrentUserService currentUserService;
    private final ImageOptimizationService imageOptimizationService;
    private final TemporaryFilePolicy temporaryFilePolicy;

    @Transactional
    public FileResponse uploadEditorFile(MultipartFile file, boolean preserveMetadata) {
        validateFile(file);
        String fileClassCode = resolveFileClassCode(file);
        Long userId = currentUserService.getUserId();

        StoredFile storedFile = fileStorage.store(fileClassCode, file, userId);
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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("파일 사이즈 제한 50MB");
        }
        String contentType = file.getContentType();
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

    private String resolveFileClassCode(MultipartFile file) {
        String contentType = file.getContentType();
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
