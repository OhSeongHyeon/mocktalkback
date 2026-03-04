package com.mocktalkback.infra.storage;

import java.time.LocalDate;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.FileStoragePathResolver;
import com.mocktalkback.domain.file.type.FileClassCode;

@Service
@Profile("test")
public class InMemoryFileStorageService implements FileStorage {

    private final FileStoragePathResolver pathResolver;
    private final Map<String, StoredObject> store = new ConcurrentHashMap<>();

    public InMemoryFileStorageService(FileStoragePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public StoredFile store(String fileClassCode, MultipartFile file, Long ownerId) {
        validateFile(fileClassCode, file);
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 소유자 식별자가 비어있습니다.");
        }
        String originalName = resolveOriginalFileName(file);
        String sanitizedOriginalName = sanitizeFileNameForStorage(originalName);
        String savedName = resolveStoredFileName(fileClassCode, sanitizedOriginalName);
        String fileNameForDatabase = resolveFileNameForDatabase(fileClassCode, originalName, savedName);
        String category = pathResolver.resolveCategory(fileClassCode);
        String storageKey = buildStorageKey(category, ownerId, savedName);
        try {
            byte[] bytes = file.getBytes();
            store.put(storageKey, new StoredObject(bytes, file.getContentType()));
            return new StoredFile(fileNameForDatabase, storageKey, (long) bytes.length, file.getContentType());
        } catch (Exception ex) {
            throw new IllegalStateException("파일 저장에 실패했습니다.");
        }
    }

    @Override
    public byte[] read(String storageKey) {
        StoredObject stored = store.get(normalizeKey(storageKey));
        if (stored == null) {
            throw new IllegalStateException("파일을 찾을 수 없습니다.");
        }
        return stored.bytes();
    }

    @Override
    public void write(String storageKey, byte[] bytes, String mimeType) {
        if (bytes == null) {
            throw new IllegalArgumentException("저장할 파일 바이트가 비어있습니다.");
        }
        store.put(normalizeKey(storageKey), new StoredObject(bytes, mimeType));
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        store.remove(normalizeKey(storageKey));
    }

    @Override
    public String resolveViewUrl(String storageKey) {
        return "/" + normalizeKey(storageKey);
    }

    @Override
    public String resolveDownloadUrl(String storageKey, String fileName, String mimeType) {
        String normalizedKey = normalizeKey(storageKey);
        String resolvedFileName = StringUtils.hasText(fileName) ? fileName : "attachment";
        String encodedFileName = URLEncoder.encode(resolvedFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "/" + normalizedKey + "?download=1&filename=" + encodedFileName;
    }

    @Override
    public PresignedUploadUrl createPresignedUploadUrl(String storageKey, String mimeType) {
        String normalizedKey = normalizeKey(storageKey);
        return new PresignedUploadUrl(
            "/storage/" + normalizedKey,
            "PUT",
            StringUtils.hasText(mimeType) ? Map.of("Content-Type", mimeType) : Map.of(),
            Instant.now().plusSeconds(300)
        );
    }

    @Override
    public StoredObjectMeta stat(String storageKey) {
        StoredObject stored = store.get(normalizeKey(storageKey));
        if (stored == null) {
            throw new IllegalStateException("파일을 찾을 수 없습니다.");
        }
        return new StoredObjectMeta((long) stored.bytes().length, stored.mimeType(), null);
    }

    private void validateFile(String fileClassCode, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }
        if (FileClassCode.PROFILE_IMAGE.equals(fileClassCode)
            || FileClassCode.BOARD_IMAGE.equals(fileClassCode)
            || FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)) {
            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
            }
            return;
        }
        if (FileClassCode.ARTICLE_CONTENT_VIDEO.equals(fileClassCode)) {
            String contentType = file.getContentType();
            if (!"video/mp4".equals(contentType) && !"video/webm".equals(contentType)) {
                throw new IllegalArgumentException("MP4 또는 WebM 영상만 업로드할 수 있습니다.");
            }
        }
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
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
        return "uploads/" + category + "/" + ownerId + "/" + year + "/" + month + "/" + day + "/" + savedName;
    }

    private String normalizeKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new IllegalArgumentException("저장소 키가 비어있습니다.");
        }
        return storageKey.trim().replace('\\', '/').replaceAll("^/+", "");
    }

    private record StoredObject(
        byte[] bytes,
        String mimeType
    ) {
    }
}
