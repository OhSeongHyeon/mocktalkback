package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private ImageVariantPolicy variantPolicy;

    @Mock
    private ObjectProvider<ImageOptimizationService> selfProvider;

    @Mock
    private FileStorage fileStorage;

    private ImageOptimizationService imageOptimizationService;

    @BeforeEach
    void setUp() {
        imageOptimizationService = new ImageOptimizationService(
            fileRepository,
            fileVariantRepository,
            variantPolicy,
            selfProvider,
            fileStorage
        );
    }

    // 원본 메타데이터 제거 처리 시 스토리지 재저장이 수행되어야 한다.
    @Test
    void processOriginal_strips_metadata_and_updates_storage() throws IOException {
        ImageIO.scanForPlugins();
        Assumptions.assumeTrue(ImageIO.getImageWritersByFormatName("png").hasNext());

        // Given: 원본 PNG 바이트와 여분 바이트
        byte[] baseImage = createPngBytes(32, 16);
        byte[] paddedImage = Arrays.copyOf(baseImage, baseImage.length + 2048);
        String storageKey = "uploads/profile/1/2026/02/08/original.png";

        when(fileStorage.read(storageKey)).thenReturn(paddedImage);

        StoredFile storedFile = new StoredFile(
            "original.png",
            storageKey,
            (long) paddedImage.length,
            "image/png"
        );

        // When: 메타데이터 제거 처리 실행
        ImageOptimizationService.OriginalFileResult result = imageOptimizationService.processOriginal(storedFile, false);

        // Then: 스토리지 갱신과 크기 축소 확인
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorage).write(eq(storageKey), bytesCaptor.capture(), eq("image/png"));
        assertThat(result.metadataPreserved()).isFalse();
        assertThat(result.fileSize()).isLessThanOrEqualTo(paddedImage.length);
        assertThat(bytesCaptor.getValue().length).isEqualTo(result.fileSize());
    }

    // 변환본 생성 요청 시 지정된 규격의 WEBP 변환본이 저장되어야 한다.
    @Test
    void createVariantsAsync_creates_variant_and_saves() throws IOException {
        ImageIO.scanForPlugins();
        Assumptions.assumeTrue(ImageIO.getImageWritersByFormatName("webp").hasNext());

        // Given: 원본 이미지 바이트와 변환 규격
        byte[] originalBytes = createPngBytes(400, 200);
        String originalStorageKey = "uploads/article_content_image/1/2026/02/08/sample.png";

        FileClassEntity fileClass = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_CONTENT_IMAGE)
            .name("게시글 본문 이미지")
            .description("테스트용")
            .mediaKind(MediaKind.IMAGE)
            .build();

        FileEntity file = FileEntity.builder()
            .fileClass(fileClass)
            .fileName("sample.png")
            .storageKey(originalStorageKey)
            .fileSize((long) originalBytes.length)
            .mimeType("image/png")
            .metadataPreserved(false)
            .build();
        ReflectionTestUtils.setField(file, "id", 10L);

        when(fileRepository.findById(10L)).thenReturn(Optional.of(file));
        when(fileStorage.read(originalStorageKey)).thenReturn(originalBytes);
        when(variantPolicy.resolve(FileClassCode.ARTICLE_CONTENT_IMAGE))
            .thenReturn(List.of(new VariantSpec(FileVariantCode.THUMB, 120)));
        when(fileVariantRepository.findByFileIdAndVariantCodeAndDeletedAtIsNull(10L, FileVariantCode.THUMB))
            .thenReturn(Optional.empty());

        ArgumentCaptor<FileVariantEntity> variantCaptor = ArgumentCaptor.forClass(FileVariantEntity.class);
        when(fileVariantRepository.save(variantCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 변환본 생성 실행
        imageOptimizationService.createVariantsAsync(10L);

        // Then: 변환본 저장 및 스토리지 업로드 호출 확인
        FileVariantEntity saved = variantCaptor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getVariantCode()).isEqualTo(FileVariantCode.THUMB);
        assertThat(saved.getMimeType()).isEqualTo("image/webp");
        assertThat(saved.getStorageKey()).contains("/variants/");
        assertThat(saved.getWidth()).isEqualTo(120);
        assertThat(saved.getHeight()).isEqualTo(60);

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorage).write(eq(saved.getStorageKey()), bytesCaptor.capture(), eq("image/webp"));
        assertThat(saved.getFileSize()).isEqualTo(bytesCaptor.getValue().length);
    }

    private byte[] createPngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(10, 20, 30).getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
