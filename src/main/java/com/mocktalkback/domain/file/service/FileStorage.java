package com.mocktalkback.domain.file.service;

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

    record StoredFile(
        String fileName,
        String storageKey,
        Long fileSize,
        String mimeType
    ) {
    }
}
