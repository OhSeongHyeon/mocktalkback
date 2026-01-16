package com.mocktalkback.domain.file.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.file.type.FileClassCode;

@Service
public class LocalFileStorageService implements FileStorage {

    private final Path basePath;
    private final String storageDirName;
    private final FileStoragePathResolver pathResolver;

    public LocalFileStorageService(
        @Value("${app.file.storage-dir:uploads}") String storageDir,
        FileStoragePathResolver pathResolver
    ) {
        this.storageDirName = normalizeStorageDir(storageDir);
        this.basePath = Paths.get(this.storageDirName).toAbsolutePath().normalize();
        this.pathResolver = pathResolver;
    }

    @Override
    public StoredFile store(String fileClassCode, MultipartFile file, Long ownerId) {
        validateFile(fileClassCode, file);
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 소유자 식별자가 비어있습니다.");
        }

        String originalName = cleanFileName(file);
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + originalName;

        String category = pathResolver.resolveCategory(fileClassCode);
        Path relativePath = buildRelativePath(category, ownerId, savedName);
        Path target = basePath.resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("파일 저장에 실패했습니다.");
        }

        String storageKey = storageDirName + "/" + relativePath.toString().replace('\\', '/');

        return new StoredFile(
            savedName,
            storageKey,
            file.getSize(),
            file.getContentType()
        );
    }

    private void validateFile(String fileClassCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }
        if (FileClassCode.PROFILE_IMAGE.equals(fileClassCode)
            || FileClassCode.BOARD_IMAGE.equals(fileClassCode)) {
            validateImage(file);
            return;
        }
        if (FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)) {
            validateImage(file);
            return;
        }
        if (FileClassCode.ARTICLE_CONTENT_VIDEO.equals(fileClassCode)) {
            validateVideo(file);
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private void validateVideo(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("영상 파일만 업로드할 수 있습니다.");
        }
        if (!"video/mp4".equals(contentType) && !"video/webm".equals(contentType)) {
            throw new IllegalArgumentException("MP4 또는 WebM 영상만 업로드할 수 있습니다.");
        }
    }

    private String cleanFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "profile");
        String cleaned = StringUtils.cleanPath(original).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(cleaned)) {
            return "profile";
        }
        return cleaned;
    }

    private Path buildRelativePath(String category, Long ownerId, String savedName) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());
        return Paths.get(category, ownerId.toString(), year, month, day, savedName);
    }

    private String normalizeStorageDir(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "uploads";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("./")) {
            trimmed = trimmed.substring(2);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!StringUtils.hasText(trimmed)) {
            return "uploads";
        }
        return trimmed;
    }
}
