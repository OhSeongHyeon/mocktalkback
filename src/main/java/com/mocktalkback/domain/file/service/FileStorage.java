package com.mocktalkback.domain.file.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    /**
     * 파일을 저장하고 저장 결과를 반환합니다.
     */
    StoredFile store(String fileClassCode, MultipartFile file, Long ownerId);

    /**
     * 저장소 키로 파일 바이트를 조회합니다.
     */
    byte[] read(String storageKey);

    /**
     * 지정된 저장소 키에 파일 바이트를 저장합니다.
     */
    void write(String storageKey, byte[] bytes, String mimeType);

    /**
     * 저장소 키에 해당하는 파일을 삭제합니다.
     */
    void delete(String storageKey);

    /**
     * 파일 조회용 URL을 반환합니다.
     */
    String resolveViewUrl(String storageKey);

    /**
     * 파일 다운로드용 URL을 반환합니다.
     */
    String resolveDownloadUrl(String storageKey, String fileName, String mimeType);

    /**
     * 클라이언트 직접 업로드용 Presigned URL을 반환합니다.
     */
    default PresignedUploadUrl createPresignedUploadUrl(String storageKey, String mimeType) {
        throw new UnsupportedOperationException("Presigned 업로드를 지원하지 않습니다.");
    }

    /**
     * 저장소 객체 메타데이터를 조회합니다.
     */
    default StoredObjectMeta stat(String storageKey) {
        throw new UnsupportedOperationException("저장소 메타 조회를 지원하지 않습니다.");
    }

    record StoredFile(
        String fileName,
        String storageKey,
        Long fileSize,
        String mimeType
    ) {
    }

    record PresignedUploadUrl(
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt
    ) {
    }

    record StoredObjectMeta(
        Long fileSize,
        String mimeType,
        String eTag
    ) {
    }
}
