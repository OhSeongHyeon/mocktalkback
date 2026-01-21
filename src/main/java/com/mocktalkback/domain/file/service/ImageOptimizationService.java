package com.mocktalkback.domain.file.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        Path originalPath = resolvePath(storedFile.storageKey());
        boolean stripped = stripMetadata(originalPath, mimeType);
        boolean metadataPreserved = !stripped;
        long fileSize = storedFile.fileSize();
        if (stripped) {
            try {
                fileSize = Files.size(originalPath);
            } catch (IOException ex) {
                log.warn("파일 크기 갱신 실패: {}", originalPath, ex);
            }
        }
        return new OriginalFileResult(fileSize, mimeType, metadataPreserved);
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
        Path originalPath = resolvePath(file.getStorageKey());
        if (!Files.exists(originalPath)) {
            log.warn("원본 파일이 존재하지 않습니다: {}", originalPath);
            return;
        }
        BufferedImage original;
        try {
            original = ImageIO.read(originalPath.toFile());
        } catch (IOException ex) {
            log.warn("이미지 로드 실패: {}", originalPath, ex);
            return;
        }
        if (original == null) {
            log.warn("이미지 로드 실패(알 수 없는 포맷): {}", originalPath);
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
            Path variantPath = resolvePath(variantStorageKey);
            try {
                Files.createDirectories(variantPath.getParent());
                Thumbnails.of(original)
                    .size(target[0], target[1])
                    .outputFormat("webp")
                    .outputQuality(WEBP_QUALITY)
                    .toFile(variantPath.toFile());
                long fileSize = Files.size(variantPath);
                FileVariantEntity variant = FileVariantEntity.builder()
                    .file(file)
                    .variantCode(spec.code())
                    .storageKey(variantStorageKey)
                    .fileSize(fileSize)
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

    private boolean stripMetadata(Path originalPath, String mimeType) {
        String format = resolveFormat(mimeType, originalPath);
        if (!isWritableFormat(format)) {
            return false;
        }
        Path tempPath = originalPath.resolveSibling(originalPath.getFileName() + ".tmp");
        Path tempOutputPath = resolveTempOutputPath(originalPath, format);
        try {
            long originalSize = resolveFileSize(originalPath);
            boolean written = writeMetadataStrippedFile(originalPath, tempOutputPath, format);
            if (!written) {
                return false;
            }
            long tempSize = Files.size(tempOutputPath);
            if (originalSize > 0 && tempSize > originalSize) {
                log.warn("메타데이터 제거 결과가 원본보다 큽니다. 원본 유지: {} ({} -> {})", originalPath, originalSize, tempSize);
                return false;
            }
            Files.move(tempOutputPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            log.warn("메타데이터 제거 실패: {}", originalPath, ex);
            return false;
        } finally {
            deleteTempFile(tempPath);
            deleteTempFile(tempOutputPath);
        }
    }

    private String resolveFormat(String mimeType, Path originalPath) {
        if (mimeType == null) {
            return resolveFormatFromPath(originalPath);
        }
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
        return resolveFormatFromPath(originalPath);
    }

    private String resolveFormatFromPath(Path path) {
        if (path == null) {
            return null;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return null;
        }
        return name.substring(dotIndex + 1);
    }

    private boolean writeMetadataStrippedFile(Path source, Path target, String format) throws IOException {
        if ("png".equalsIgnoreCase(format)) {
            return writePngWithoutMetadata(source, target);
        }
        Thumbnails.of(source.toFile())
            .scale(1.0)
            .outputFormat(format)
            .outputQuality(1.0f)
            .toFile(target.toFile());
        return true;
    }

    private boolean writePngWithoutMetadata(Path source, Path target) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            log.warn("이미지 로드 실패(알 수 없는 포맷): {}", source);
            return false;
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            log.warn("PNG ImageWriter가 없습니다: {}", source);
            return false;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        if (params.canWriteCompressed()) {
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.0f);
        }
        ImageOutputStream output = ImageIO.createImageOutputStream(target.toFile());
        if (output == null) {
            writer.dispose();
            log.warn("PNG 출력 스트림을 생성할 수 없습니다: {}", target);
            return false;
        }
        try (output) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return true;
    }

    private long resolveFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            log.warn("파일 크기 조회 실패: {}", path, ex);
            return -1L;
        }
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("임시 파일 정리 실패: {}", path, ex);
        }
    }

    private Path resolveTempOutputPath(Path originalPath, String format) {
        String fileName = originalPath.getFileName().toString();
        String extension = format == null ? "" : "." + format;
        return originalPath.resolveSibling(fileName + ".tmp" + extension);
    }

    private boolean isWritableFormat(String format) {
        if (format == null || format.isBlank()) {
            return false;
        }
        return !"gif".equals(format);
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

    private Path resolvePath(String storageKey) {
        return Paths.get(storageKey).toAbsolutePath().normalize();
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
