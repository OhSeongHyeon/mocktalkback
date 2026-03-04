package com.mocktalkback.domain.file.service;

public enum StorageDeleteSource {
    UPLOAD_CANCEL,
    UPLOAD_COMPLETE_ROLLBACK,
    UPLOAD_ORPHAN_CLEANUP,
    TEMP_FILE_CLEANUP
}

