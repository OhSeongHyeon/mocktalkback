package com.mocktalkback.infra.storage;

import java.time.LocalDate;
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
        String originalName = cleanFileName(file);
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + originalName;
        String category = pathResolver.resolveCategory(fileClassCode);
        String storageKey = buildStorageKey(category, ownerId, savedName);
        try {
            byte[] bytes = file.getBytes();
            store.put(storageKey, new StoredObject(bytes, file.getContentType()));
            return new StoredFile(savedName, storageKey, (long) bytes.length, file.getContentType());
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

    private String cleanFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String cleaned = StringUtils.cleanPath(original).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(cleaned)) {
            return "file";
        }
        return cleaned;
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
