package com.mocktalkback.domain.file.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.service.ImageVariantPolicy.VariantSpec;
import com.mocktalkback.domain.file.type.FileVariantCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOptimizationService {

    private static final float WEBP_QUALITY = 0.8f;
    private static final AtomicBoolean PLUGINS_SCANNED = new AtomicBoolean(false);

    private final FileRepository fileRepository;
    private final FileVariantRepository fileVariantRepository;
    private final ImageVariantPolicy variantPolicy;
    private final ObjectProvider<ImageOptimizationService> selfProvider;
    private final FileStorage fileStorage;

    public OriginalFileResult processOriginal(StoredFile storedFile, boolean preserveMetadata) {
        if (storedFile == null) {
            throw new IllegalArgumentException("저장된 파일 정보가 비어있습니다.");
        }
        String mimeType = storedFile.mimeType();
        if (!isImage(mimeType)) {
            return new OriginalFileResult(storedFile.fileSize(), mimeType, false);
        }
        if (preserveMetadata) {
            return new OriginalFileResult(storedFile.fileSize(), mimeType, true);
        }
        ensureImageIoPluginsLoaded();
        byte[] originalBytes = fileStorage.read(storedFile.storageKey());
        String format = resolveFormat(mimeType, storedFile.fileName());
        if (!isWritableFormat(format)) {
            return new OriginalFileResult((long) originalBytes.length, mimeType, true);
        }
        byte[] strippedBytes = stripMetadata(originalBytes, format);
        if (strippedBytes == null) {
            return new OriginalFileResult((long) originalBytes.length, mimeType, true);
        }
        if (originalBytes.length > 0 && strippedBytes.length > originalBytes.length) {
            log.warn(
                "메타데이터 제거 결과가 원본보다 큽니다. 원본 유지: {} ({} -> {})",
                storedFile.storageKey(),
                originalBytes.length,
                strippedBytes.length
            );
            return new OriginalFileResult((long) originalBytes.length, mimeType, true);
        }
        boolean stripped = !Arrays.equals(originalBytes, strippedBytes);
        if (stripped) {
            fileStorage.write(storedFile.storageKey(), strippedBytes, mimeType);
            return new OriginalFileResult((long) strippedBytes.length, mimeType, false);
        }
        return new OriginalFileResult((long) originalBytes.length, mimeType, true);
    }

    public void enqueueVariantGeneration(FileEntity fileEntity) {
        if (fileEntity == null || fileEntity.getId() == null) {
            return;
        }
        Long fileId = fileEntity.getId();
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            invokeAsync(fileId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invokeAsync(fileId);
            }
        });
    }

    @Async("imageTaskExecutor")
    @Transactional
    public void createVariantsAsync(Long fileId) {
        if (fileId == null) {
            return;
        }
        Optional<FileEntity> optional = fileRepository.findById(fileId);
        if (optional.isEmpty()) {
            return;
        }
        FileEntity file = optional.get();
        if (file.isDeleted()) {
            return;
        }
        String mimeType = file.getMimeType();
        if (!isOptimizableImage(mimeType)) {
            return;
        }
        List<VariantSpec> specs = variantPolicy.resolve(file.getFileClass().getCode());
        if (specs.isEmpty()) {
            return;
        }
        ensureImageIoPluginsLoaded();
        byte[] originalBytes;
        try {
            originalBytes = fileStorage.read(file.getStorageKey());
        } catch (Exception ex) {
            log.warn("원본 파일 로드 실패: {}", file.getStorageKey(), ex);
            return;
        }
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        } catch (IOException ex) {
            log.warn("이미지 로드 실패: {}", file.getStorageKey(), ex);
            return;
        }
        if (original == null) {
            log.warn("이미지 로드 실패(알 수 없는 포맷): {}", file.getStorageKey());
            return;
        }
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        for (VariantSpec spec : specs) {
            if (fileVariantRepository.findByFileIdAndVariantCodeAndDeletedAtIsNull(fileId, spec.code()).isPresent()) {
                continue;
            }
            int[] target = resolveTargetSize(originalWidth, originalHeight, spec.maxSize());
            String variantStorageKey = buildVariantStorageKey(file.getStorageKey(), spec.code());
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Thumbnails.of(original)
                    .size(target[0], target[1])
                    .outputFormat("webp")
                    .outputQuality(WEBP_QUALITY)
                    .toOutputStream(output);
                byte[] variantBytes = output.toByteArray();
                fileStorage.write(variantStorageKey, variantBytes, "image/webp");
                FileVariantEntity variant = FileVariantEntity.builder()
                    .file(file)
                    .variantCode(spec.code())
                    .storageKey(variantStorageKey)
                    .fileSize((long) variantBytes.length)
                    .mimeType("image/webp")
                    .width(target[0])
                    .height(target[1])
                    .build();
                fileVariantRepository.save(variant);
            } catch (IOException ex) {
                log.warn("변환본 생성 실패: {} {}", fileId, spec.code(), ex);
            }
        }
    }

    private byte[] stripMetadata(byte[] originalBytes, String format) {
        try {
            return writeMetadataStrippedFile(originalBytes, format);
        } catch (IOException ex) {
            log.warn("메타데이터 제거 실패", ex);
            return null;
        }
    }

    private String resolveFormat(String mimeType, String fileName) {
        if (mimeType != null) {
            String normalized = mimeType.toLowerCase(Locale.ROOT);
            if (normalized.contains("jpeg") || normalized.contains("jpg")) {
                return "jpg";
            }
            if (normalized.contains("png")) {
                return "png";
            }
            if (normalized.contains("webp")) {
                return "webp";
            }
            if (normalized.contains("gif")) {
                return "gif";
            }
        }
        return resolveFormatFromFileName(fileName);
    }

    private String resolveFormatFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0) {
            return null;
        }
        return normalized.substring(dotIndex + 1);
    }

    private byte[] writeMetadataStrippedFile(byte[] source, String format) throws IOException {
        if ("png".equalsIgnoreCase(format)) {
            return writePngWithoutMetadata(source);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(source))
            .scale(1.0)
            .outputFormat(format)
            .outputQuality(1.0f)
            .toOutputStream(output);
        return output.toByteArray();
    }

    private byte[] writePngWithoutMetadata(byte[] source) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
        if (image == null) {
            log.warn("PNG 이미지 로드 실패(알 수 없는 포맷)");
            return null;
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            log.warn("PNG ImageWriter가 없습니다.");
            return null;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        if (params.canWriteCompressed()) {
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.0f);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output);
        if (imageOutput == null) {
            writer.dispose();
            log.warn("PNG 출력 스트림을 생성할 수 없습니다.");
            return null;
        }
        try (imageOutput) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private boolean isWritableFormat(String format) {
        if (format == null || format.isBlank()) {
            return false;
        }
        return !"gif".equalsIgnoreCase(format);
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private boolean isOptimizableImage(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("image/")
            && !normalized.contains("gif");
    }

    private int[] resolveTargetSize(int width, int height, int maxSize) {
        int longest = Math.max(width, height);
        if (longest <= maxSize) {
            return new int[] { width, height };
        }
        double ratio = (double) maxSize / (double) longest;
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));
        return new int[] { targetWidth, targetHeight };
    }

    private String buildVariantStorageKey(String originalStorageKey, FileVariantCode variantCode) {
        if (originalStorageKey == null || originalStorageKey.isBlank()) {
            throw new IllegalArgumentException("원본 저장 경로가 비어있습니다.");
        }
        String normalized = originalStorageKey.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex < 0) {
            return normalized + "_variants/" + variantFileName(normalized, variantCode);
        }
        String dir = normalized.substring(0, slashIndex);
        String fileName = normalized.substring(slashIndex + 1);
        return dir + "/variants/" + variantFileName(fileName, variantCode);
    }

    private String variantFileName(String originalFileName, FileVariantCode variantCode) {
        String baseName = originalFileName;
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
        }
        return baseName + "_" + variantCode.name().toLowerCase(Locale.ROOT) + ".webp";
    }

    private void ensureImageIoPluginsLoaded() {
        if (PLUGINS_SCANNED.compareAndSet(false, true)) {
            ImageIO.scanForPlugins();
        }
    }

    private void invokeAsync(Long fileId) {
        ImageOptimizationService proxy = selfProvider.getIfAvailable();
        if (proxy != null) {
            proxy.createVariantsAsync(fileId);
            return;
        }
        createVariantsAsync(fileId);
    }

    public record OriginalFileResult(
        long fileSize,
        String mimeType,
        boolean metadataPreserved
    ) {
    }
}
