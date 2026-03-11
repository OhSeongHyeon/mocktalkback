package com.mocktalkback.domain.article.service;

import org.springframework.stereotype.Service;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.service.EditorFileService;
import com.mocktalkback.domain.file.service.FileStorage;
import com.mocktalkback.domain.file.service.StorageDeleteRetryService;
import com.mocktalkback.domain.file.service.StorageDeleteSource;
import com.mocktalkback.domain.file.upload.service.UploadStorageKeyFactory;
import com.mocktalkback.domain.file.upload.type.UploadPurpose;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleImportAssetStorageService {

    private final FileStorage fileStorage;
    private final UploadStorageKeyFactory uploadStorageKeyFactory;
    private final EditorFileService editorFileService;
    private final StorageDeleteRetryService storageDeleteRetryService;

    public FileResponse storeEditorAsset(Long ownerId, String originalFileName, byte[] bytes, String mimeType) {
        if (ownerId == null) {
            throw new IllegalArgumentException("파일 소유자 식별자가 비어 있습니다.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("업로드할 assets 바이트가 비어 있습니다.");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("업로드할 assets MIME 타입이 비어 있습니다.");
        }

        UploadPurpose purpose = mimeType.startsWith("image/")
            ? UploadPurpose.EDITOR_IMAGE
            : UploadPurpose.EDITOR_VIDEO;
        UploadStorageKeyFactory.PreparedUploadFile preparedFile = uploadStorageKeyFactory.prepare(
            purpose.toFileClassCode(),
            ownerId,
            originalFileName
        );

        fileStorage.write(preparedFile.storageKey(), bytes, mimeType);
        try {
            return editorFileService.completeEditorFileUpload(
                new FileStorage.StoredFile(
                    preparedFile.fileNameForDatabase(),
                    preparedFile.storageKey(),
                    (long) bytes.length,
                    mimeType
                ),
                false
            );
        } catch (RuntimeException exception) {
            storageDeleteRetryService.deleteNowOrEnqueue(
                preparedFile.storageKey(),
                StorageDeleteSource.UPLOAD_COMPLETE_ROLLBACK,
                "article-import-asset"
            );
            throw exception;
        }
    }
}
