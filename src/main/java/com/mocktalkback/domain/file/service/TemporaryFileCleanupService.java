package com.mocktalkback.domain.file.service;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.article.repository.ArticleFileRepository;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.type.FileClassCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryFileCleanupService {

    private final FileRepository fileRepository;
    private final FileVariantRepository fileVariantRepository;
    private final ArticleFileRepository articleFileRepository;

    @Scheduled(fixedDelayString = "${app.file.temp-cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredTemporaryFiles() {
        Instant now = Instant.now();
        List<FileEntity> expired = fileRepository.findAllByTempExpiresAtBeforeAndDeletedAtIsNull(now);
        for (FileEntity file : expired) {
            Long fileId = file.getId();
            if (fileId == null) {
                continue;
            }
            if (!isEditorFile(file)) {
                file.clearTemporary();
                continue;
            }
            if (articleFileRepository.existsByFileId(fileId)) {
                file.clearTemporary();
                continue;
            }
            softDeleteVariants(fileId);
            file.softDelete();
            log.info("임시 파일 정리 완료: {}", fileId);
        }
    }

    private void softDeleteVariants(Long fileId) {
        List<FileVariantEntity> variants = fileVariantRepository.findAllByFileIdAndDeletedAtIsNull(fileId);
        for (FileVariantEntity variant : variants) {
            variant.softDelete();
        }
    }

    private boolean isEditorFile(FileEntity file) {
        if (file == null || file.getFileClass() == null) {
            return false;
        }
        String code = file.getFileClass().getCode();
        return FileClassCode.ARTICLE_CONTENT_IMAGE.equals(code)
            || FileClassCode.ARTICLE_CONTENT_VIDEO.equals(code);
    }
}
