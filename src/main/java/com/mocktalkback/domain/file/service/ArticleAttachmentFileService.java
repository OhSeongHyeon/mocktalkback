package com.mocktalkback.domain.file.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
public class ArticleAttachmentFileService {

    private static final long MAX_UPLOAD_SIZE = 50L * 1024L * 1024L;

    // 운영에서 허용할 첨부파일 확장자 목록(소문자 기준)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "hwp", "hwpx",
        "txt", "csv", "zip", "7z", "rar",
        "jpg", "jpeg", "png", "webp", "gif",
        "mp4", "webm", "mp3", "wav"
    );

    // MIME은 허용 목록 기반으로 검증한다.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/x-hwp",
        "application/vnd.hancom.hwp",
        "application/haansofthwp",
        "application/haansofthwpx",
        "application/vnd.hancom.hwpx",
        "text/plain",
        "text/csv",
        "application/csv",
        "application/zip",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-rar-compressed",
        "application/vnd.rar",
        "image/jpg",
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif",
        "video/mp4",
        "video/webm",
        "audio/mpeg",
        "audio/wav",
        "audio/x-wav"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        "ade", "adp", "apk", "appx", "bat", "cmd", "com", "cpl", "dll", "exe", "hta",
        "ins", "isp", "jar", "js", "jse", "lnk", "msc", "msi", "msp", "mst", "pif",
        "ps1", "reg", "scr", "sh", "vb", "vbe", "vbs", "ws", "wsc", "wsf", "wsh"
    );

    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-dosexec",
        "application/x-sh",
        "application/x-bat",
        "application/x-shellscript"
    );

    private final FileStorage fileStorage;
    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileMapper fileMapper;
    private final CurrentUserService currentUserService;
    private final ImageOptimizationService imageOptimizationService;
    private final TemporaryFilePolicy temporaryFilePolicy;

    @Transactional
    public FileResponse uploadArticleAttachmentFile(MultipartFile file, boolean preserveMetadata) {
        validateFile(file);
        Long userId = currentUserService.getUserId();
        String fileClassCode = FileClassCode.ARTICLE_ATTACHMENT;

        StoredFile storedFile = fileStorage.store(fileClassCode, file, userId);
        return completeArticleAttachmentFileUpload(storedFile, preserveMetadata);
    }

    @Transactional
    public FileResponse completeArticleAttachmentFileUpload(StoredFile storedFile, boolean preserveMetadata) {
        validateStoredFile(storedFile);

        // 첨부파일은 원본 보존을 우선한다(파일명/바이트 원본 유지).
        boolean preserveOriginalAttachment = true;
        ImageOptimizationService.OriginalFileResult processed = imageOptimizationService
            .processOriginal(storedFile, preserveOriginalAttachment);
        FileClassEntity fileClass = getOrCreateFileClass();

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

        String fileName = storedFile.fileName();
        String extension = normalizeExtension(resolveExtension(fileName));
        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("업로드할 수 없는 확장자입니다.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }

        String contentType = storedFile.mimeType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
        String normalizedContentType = normalizeMimeType(contentType);
        if (BLOCKED_MIME_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("업로드할 수 없는 파일 형식입니다.");
        }
        if ("application/octet-stream".equals(normalizedContentType)) {
            return;
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("파일 사이즈 제한 50MB");
        }

        String extension = normalizeExtension(resolveExtension(file.getOriginalFilename()));
        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("업로드할 수 없는 확장자입니다.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
        String normalizedContentType = normalizeMimeType(contentType);
        if (BLOCKED_MIME_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("업로드할 수 없는 파일 형식입니다.");
        }
        if ("application/octet-stream".equals(normalizedContentType)) {
            return;
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        String normalized = originalFilename.trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(dotIndex + 1);
    }

    private String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return null;
        }
        return extension.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String contentType) {
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex).trim();
        }
        return normalized;
    }

    private FileClassEntity getOrCreateFileClass() {
        Optional<FileClassEntity> optional = fileClassRepository.findByCode(FileClassCode.ARTICLE_ATTACHMENT);
        if (optional.isPresent()) {
            return optional.get();
        }
        FileClassEntity created = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_ATTACHMENT)
            .name("게시글 첨부파일")
            .description("게시글에 첨부되는 일반 파일")
            .mediaKind(MediaKind.ANY)
            .build();
        return fileClassRepository.save(created);
    }
}
