package com.mocktalkback.domain.file.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mocktalkback.domain.file.service.FileStoragePathResolver;
import com.mocktalkback.domain.file.type.FileClassCode;

@ExtendWith(MockitoExtension.class)
class UploadStorageKeyFactoryTest {

    private static final Pattern UUID_WITH_EXTENSION = Pattern.compile("^[a-f0-9]{32}\\.[a-zA-Z0-9]+$");
    private static final Pattern UUID_ONLY = Pattern.compile("^[a-f0-9]{32}$");

    @Mock
    private FileStoragePathResolver fileStoragePathResolver;

    private UploadStorageKeyFactory uploadStorageKeyFactory;

    @BeforeEach
    void setUp() {
        uploadStorageKeyFactory = new UploadStorageKeyFactory(fileStoragePathResolver, "uploads");
    }

    // 게시판 이미지 업로드는 storage key 파일명에 원본파일명을 포함하지 않고 uuid.ext만 사용해야 한다.
    @Test
    void prepare_board_image_uses_uuid_extension_and_keeps_original_name_for_db() {
        // given: 게시판 이미지 파일 클래스와 원본 파일명
        when(fileStoragePathResolver.resolveCategory(FileClassCode.BOARD_IMAGE)).thenReturn("board_image");

        // when: 업로드 저장 경로를 준비하면
        UploadStorageKeyFactory.PreparedUploadFile prepared = uploadStorageKeyFactory.prepare(
            FileClassCode.BOARD_IMAGE,
            7L,
            "board-banner.final.png"
        );

        // then: DB 파일명은 원본을 유지하고, storage key 파일명은 uuid.ext 형식이다.
        assertThat(prepared.fileNameForDatabase()).isEqualTo("board-banner.final.png");
        String savedName = extractSavedName(prepared.storageKey());
        assertThat(savedName).matches(UUID_WITH_EXTENSION);
        assertThat(savedName).endsWith(".png");
    }

    // 프로필 이미지 업로드도 storage key 파일명에 원본파일명을 포함하지 않아야 한다.
    @Test
    void prepare_profile_image_uses_uuid_extension_without_original_name_in_storage_key() {
        // given: 프로필 이미지 파일 클래스와 원본 파일명
        when(fileStoragePathResolver.resolveCategory(FileClassCode.PROFILE_IMAGE)).thenReturn("profile_image");

        // when: 업로드 저장 경로를 준비하면
        UploadStorageKeyFactory.PreparedUploadFile prepared = uploadStorageKeyFactory.prepare(
            FileClassCode.PROFILE_IMAGE,
            11L,
            "my profile image.jpeg"
        );

        // then: DB 파일명은 원본을 유지하고, storage key 파일명은 uuid.ext 형식이다.
        assertThat(prepared.fileNameForDatabase()).isEqualTo("my profile image.jpeg");
        String savedName = extractSavedName(prepared.storageKey());
        assertThat(savedName).matches(UUID_WITH_EXTENSION);
        assertThat(savedName).endsWith(".jpeg");
        assertThat(savedName).doesNotContain("_my");
    }

    // 확장자가 없는 파일도 storage key 파일명은 uuid만 사용해야 한다.
    @Test
    void prepare_without_extension_uses_uuid_only() {
        // given: 확장자가 없는 원본 파일명
        when(fileStoragePathResolver.resolveCategory(FileClassCode.ARTICLE_ATTACHMENT)).thenReturn("article_attachment");

        // when: 업로드 저장 경로를 준비하면
        UploadStorageKeyFactory.PreparedUploadFile prepared = uploadStorageKeyFactory.prepare(
            FileClassCode.ARTICLE_ATTACHMENT,
            5L,
            "README"
        );

        // then: DB 파일명은 원본 유지, storage key 파일명은 uuid만 사용한다.
        assertThat(prepared.fileNameForDatabase()).isEqualTo("README");
        String savedName = extractSavedName(prepared.storageKey());
        assertThat(savedName).matches(UUID_ONLY);
    }

    private String extractSavedName(String storageKey) {
        int slashIndex = storageKey.lastIndexOf('/');
        return slashIndex >= 0 ? storageKey.substring(slashIndex + 1) : storageKey;
    }
}

