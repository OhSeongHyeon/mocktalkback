package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.service.FileStorage.StoredFile;
import com.mocktalkback.domain.file.service.ImageVariantPolicy.VariantSpec;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.FileVariantCode;
import com.mocktalkback.domain.file.type.MediaKind;

@ExtendWith(MockitoExtension.class)
class ImageOptimizationServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private ImageVariantPolicy variantPolicy;

    @Mock
    private ObjectProvider<ImageOptimizationService> selfProvider;

    private ImageOptimizationService imageOptimizationService;

    @BeforeEach
    void setUp() {
        imageOptimizationService = new ImageOptimizationService(
            fileRepository,
            fileVariantRepository,
            variantPolicy,
            selfProvider
        );
    }

    // 원본 메타데이터 제거 처리 시 임시 파일이 정리되고 결과 크기가 갱신되어야 한다.
    @Test
    void processOriginal_strips_metadata_and_cleans_temp_files() throws IOException {
        ImageIO.scanForPlugins();
        Assumptions.assumeTrue(ImageIO.getImageWritersByFormatName("png").hasNext());

        // Given: 임시 PNG 파일과 여분 바이트
        Path originalPath = tempDir.resolve("original.png");
        BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(10, 20, 30).getRGB());
        ImageIO.write(image, "png", originalPath.toFile());
        Files.write(originalPath, new byte[2048], StandardOpenOption.APPEND);
        long originalSize = Files.size(originalPath);

        StoredFile storedFile = new StoredFile(
            "original.png",
            originalPath.toString(),
            originalSize,
            "image/png"
        );

        // When: 메타데이터 제거 처리 실행
        ImageOptimizationService.OriginalFileResult result =
            imageOptimizationService.processOriginal(storedFile, false);

        // Then: 처리 결과와 임시 파일 정리 확인
        assertThat(result.metadataPreserved()).isFalse();
        assertThat(result.fileSize()).isLessThanOrEqualTo(originalSize);
        assertThat(Files.size(originalPath)).isEqualTo(result.fileSize());
        assertThat(Files.exists(tempDir.resolve("original.png.tmp"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("original.png.tmp.png"))).isFalse();
    }

    // 변환본 생성 요청 시 지정된 규격의 WEBP 변환본이 저장되어야 한다.
    @Test
    void createVariantsAsync_creates_variant_and_saves() throws IOException {
        ImageIO.scanForPlugins();
        Assumptions.assumeTrue(ImageIO.getImageWritersByFormatName("webp").hasNext());

        // Given: 원본 이미지와 변환 규격
        Path originalPath = tempDir.resolve("sample.png");
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", originalPath.toFile());

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_CONTENT_IMAGE)
            .name("게시글 본문 이미지")
            .description("테스트용")
            .mediaKind(MediaKind.IMAGE)
            .build();

        FileEntity file = FileEntity.builder()
            .fileClass(fileClass)
            .fileName("sample.png")
            .storageKey(originalPath.toString())
            .fileSize(Files.size(originalPath))
            .mimeType("image/png")
            .metadataPreserved(false)
            .build();
        ReflectionTestUtils.setField(file, "id", 10L);

        when(fileRepository.findById(10L)).thenReturn(Optional.of(file));
        when(variantPolicy.resolve(FileClassCode.ARTICLE_CONTENT_IMAGE))
            .thenReturn(List.of(new VariantSpec(FileVariantCode.THUMB, 120)));
        when(fileVariantRepository.findByFileIdAndVariantCodeAndDeletedAtIsNull(10L, FileVariantCode.THUMB))
            .thenReturn(Optional.empty());

        ArgumentCaptor<FileVariantEntity> captor = ArgumentCaptor.forClass(FileVariantEntity.class);
        when(fileVariantRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 변환본 생성 실행
        imageOptimizationService.createVariantsAsync(10L);

        // Then: 변환본 저장 및 파일 생성 확인
        FileVariantEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getVariantCode()).isEqualTo(FileVariantCode.THUMB);
        assertThat(saved.getMimeType()).isEqualTo("image/webp");
        assertThat(saved.getStorageKey()).contains("/variants/");

        Path variantPath = Path.of(saved.getStorageKey());
        assertThat(Files.exists(variantPath)).isTrue();
        assertThat(saved.getFileSize()).isEqualTo(Files.size(variantPath));
        assertThat(saved.getWidth()).isEqualTo(120);
        assertThat(saved.getHeight()).isEqualTo(60);
    }
}
