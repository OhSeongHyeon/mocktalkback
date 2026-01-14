package com.mocktalkback.domain.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    /**
     * 파일을 저장하고 저장 결과를 반환합니다.
     */
    StoredFile store(String fileClassCode, MultipartFile file, Long ownerId);

    record StoredFile(
        String fileName,
        String storageKey,
        Long fileSize,
        String mimeType
    ) {
    }
}
