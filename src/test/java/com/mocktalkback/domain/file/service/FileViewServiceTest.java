package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.repository.FileVariantRepository;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.FileVariantCode;
import com.mocktalkback.domain.file.type.MediaKind;

@ExtendWith(MockitoExtension.class)
class FileViewServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileVariantRepository fileVariantRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileAccessDecisionService fileAccessDecisionService;

    // variant=original 요청은 원본 파일 URL로 조회되어야 한다.
    @Test
    void resolveViewLocation_returns_original_when_variant_is_original() {
        // given: 이미지 원본 파일과 variant=original 요청
        FileViewService fileViewService = new FileViewService(
            fileRepository,
            fileVariantRepository,
            fileStorage,
            fileAccessDecisionService
        );
        FileEntity fileEntity = createImageFileEntity("uploads/article_content_image/1/original.png");
        when(fileRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(fileEntity));
        when(fileAccessDecisionService.decide(fileEntity)).thenReturn(FileAccessDecision.protectedAccess());
        when(fileStorage.resolveProtectedViewUrl("uploads/article_content_image/1/original.png"))
            .thenReturn("https://files.mocktalk.test/uploads/article_content_image/1/original.png");

        // when: 보기 URL을 해석하면
        String location = fileViewService.resolveViewLocation(1L, "original");

        // then: 원본 URL을 반환하고 변환본 조회는 수행하지 않는다.
        assertThat(location).isEqualTo("https://files.mocktalk.test/uploads/article_content_image/1/original.png");
        verify(fileVariantRepository, never()).findByFileIdAndVariantCodeAndDeletedAtIsNull(1L, FileVariantCode.ORIGINAL_SIZE);
    }

    // variant=original_size 요청은 ORIGINAL_SIZE 변환본을 우선 사용해야 한다.
    @Test
    void resolveViewLocation_returns_original_size_variant_when_exists() {
        // given: 이미지 원본과 ORIGINAL_SIZE 변환본이 존재한다.
        FileViewService fileViewService = new FileViewService(
            fileRepository,
            fileVariantRepository,
            fileStorage,
            fileAccessDecisionService
        );
        FileEntity fileEntity = createImageFileEntity("uploads/article_content_image/10/original.png");
        FileVariantEntity variantEntity = FileVariantEntity.builder()
            .file(fileEntity)
            .variantCode(FileVariantCode.ORIGINAL_SIZE)
            .storageKey("uploads/article_content_image/10/variants/original_original_size.webp")
            .fileSize(1024L)
            .mimeType("image/webp")
            .width(1920)
            .height(1080)
            .build();

        when(fileRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(fileEntity));
        when(fileAccessDecisionService.decide(fileEntity)).thenReturn(FileAccessDecision.protectedAccess());
        when(fileVariantRepository.findByFileIdAndVariantCodeAndDeletedAtIsNull(10L, FileVariantCode.ORIGINAL_SIZE))
            .thenReturn(Optional.of(variantEntity));
        when(fileStorage.resolveProtectedViewUrl("uploads/article_content_image/10/variants/original_original_size.webp"))
            .thenReturn("https://files.mocktalk.test/uploads/article_content_image/10/variants/original_original_size.webp");

        // when: 보기 URL을 해석하면
        String location = fileViewService.resolveViewLocation(10L, "original_size");

        // then: ORIGINAL_SIZE 변환본 URL을 반환한다.
        assertThat(location).isEqualTo("https://files.mocktalk.test/uploads/article_content_image/10/variants/original_original_size.webp");
        verify(fileVariantRepository).findByFileIdAndVariantCodeAndDeletedAtIsNull(10L, FileVariantCode.ORIGINAL_SIZE);
    }

    private FileEntity createImageFileEntity(String storageKey) {
        FileClassEntity fileClassEntity = FileClassEntity.builder()
            .code(FileClassCode.ARTICLE_CONTENT_IMAGE)
            .name("게시글 본문 이미지")
            .description("테스트")
            .mediaKind(MediaKind.IMAGE)
            .build();

        return FileEntity.builder()
            .fileClass(fileClassEntity)
            .fileName("original.png")
            .storageKey(storageKey)
            .fileSize(2048L)
            .mimeType("image/png")
            .metadataPreserved(false)
            .build();
    }
}
