package com.mocktalkback.domain.file.upload.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.file.service.FileStoragePathResolver;
import com.mocktalkback.domain.file.type.FileClassCode;

@Component
public class UploadStorageKeyFactory {

    private final FileStoragePathResolver pathResolver;
    private final String keyPrefix;

    public UploadStorageKeyFactory(
        FileStoragePathResolver pathResolver,
        @Value("${app.object-storage.key-prefix:uploads}") String keyPrefix
    ) {
        this.pathResolver = pathResolver;
        this.keyPrefix = keyPrefix;
    }

    public PreparedUploadFile prepare(String fileClassCode, Long ownerId, String originalFileName) {
        if (!StringUtils.hasText(fileClassCode)) {
            throw new IllegalArgumentException("파일 분류 코드가 비어있습니다.");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 소유자 식별자가 비어있습니다.");
        }
        String resolvedOriginalName = resolveOriginalFileName(originalFileName);
        String sanitizedOriginalName = sanitizeFileNameForStorage(resolvedOriginalName);
        String savedName = resolveStoredFileName(fileClassCode, sanitizedOriginalName);
        String fileNameForDatabase = resolveFileNameForDatabase(fileClassCode, resolvedOriginalName, savedName);
        String category = pathResolver.resolveCategory(fileClassCode);
        String storageKey = buildStorageKey(category, ownerId, savedName);
        return new PreparedUploadFile(fileNameForDatabase, storageKey);
    }

    private String resolveOriginalFileName(String originalFileName) {
        String original = Objects.requireNonNullElse(originalFileName, "file");
        String cleanedPath = StringUtils.cleanPath(original).replace('\\', '/');
        int slashIndex = cleanedPath.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? cleanedPath.substring(slashIndex + 1) : cleanedPath;
        if (!StringUtils.hasText(fileName)) {
            return "file";
        }
        return fileName;
    }

    private String sanitizeFileNameForStorage(String originalName) {
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(sanitized)) {
            return "file";
        }
        return sanitized;
    }

    private String resolveStoredFileName(String fileClassCode, String sanitizedOriginalName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)
            || FileClassCode.ARTICLE_CONTENT_VIDEO.equals(fileClassCode)
            || FileClassCode.ARTICLE_ATTACHMENT.equals(fileClassCode)) {
            String extension = resolveExtension(sanitizedOriginalName);
            if (!StringUtils.hasText(extension)) {
                return uuid;
            }
            return uuid + "." + extension;
        }
        return uuid + "_" + sanitizedOriginalName;
    }

    private String resolveFileNameForDatabase(String fileClassCode, String originalName, String storedName) {
        if (FileClassCode.ARTICLE_ATTACHMENT.equals(fileClassCode)) {
            return originalName;
        }
        return storedName;
    }

    private String resolveExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1);
    }

    private String buildStorageKey(String category, Long ownerId, String savedName) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());
        String normalizedPrefix = normalizePrefix(keyPrefix);
        return normalizedPrefix + "/" + category + "/" + ownerId + "/" + year + "/" + month + "/" + day + "/" + savedName;
    }

    private String normalizePrefix(String rawPrefix) {
        if (!StringUtils.hasText(rawPrefix)) {
            return "uploads";
        }
        String normalized = rawPrefix.trim().replace('\\', '/');
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");
        if (!StringUtils.hasText(normalized)) {
            return "uploads";
        }
        return normalized;
    }

    public record PreparedUploadFile(
        String fileNameForDatabase,
        String storageKey
    ) {
    }
}
