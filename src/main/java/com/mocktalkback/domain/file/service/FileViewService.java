package com.mocktalkback.domain.file.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.type.FileVariantCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileViewService {

    private final FileRepository fileRepository;
    private final FileVariantRepository fileVariantRepository;
    private final FileStorage fileStorage;
    private final FileAccessDecisionService fileAccessDecisionService;

    public String resolveViewLocation(Long fileId, String variantParam) {
        FileEntity file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다."));

        FileAccessDecision accessDecision = fileAccessDecisionService.decide(file);
        if (!accessDecision.allowed()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        FileVariantCode variantCode = resolveVariantCode(variantParam);
        if (variantCode == null || !isImage(file.getMimeType())) {
            return resolveDeliveryUrl(file.getStorageKey(), accessDecision.deliveryMode());
        }

        Optional<FileVariantEntity> variant = fileVariantRepository
            .findByFileIdAndVariantCodeAndDeletedAtIsNull(fileId, variantCode);
        if (variant.isPresent()) {
            return resolveDeliveryUrl(variant.get().getStorageKey(), accessDecision.deliveryMode());
        }
        return resolveDeliveryUrl(file.getStorageKey(), accessDecision.deliveryMode());
    }

    private FileVariantCode resolveVariantCode(String variantParam) {
        if (variantParam == null || variantParam.isBlank()) {
            return FileVariantCode.MEDIUM;
        }
        String normalized = variantParam.trim().toLowerCase(Locale.ROOT);
        if ("original".equals(normalized)) {
            return null;
        }
        try {
            return FileVariantCode.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 변환본 코드입니다.");
        }
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private String resolveDeliveryUrl(String storageKey, FileDeliveryMode deliveryMode) {
        if (deliveryMode == FileDeliveryMode.PROTECTED) {
            return fileStorage.resolveProtectedViewUrl(storageKey);
        }
        return fileStorage.resolveViewUrl(storageKey);
    }

}
