package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mocktalkback.domain.file.service.ImageVariantPolicy.VariantSpec;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.FileVariantCode;

class ImageVariantPolicyTest {

    private final ImageVariantPolicy imageVariantPolicy = new ImageVariantPolicy();

    // 게시글 본문 이미지는 THUMB와 ORIGINAL_SIZE 정책만 반환해야 한다.
    @Test
    void resolve_article_content_image_returns_thumb_and_original_size() {
        // given: 게시글 본문 이미지 파일 클래스 코드
        String fileClassCode = FileClassCode.ARTICLE_CONTENT_IMAGE;

        // when: 변환본 정책을 조회하면
        List<VariantSpec> specs = imageVariantPolicy.resolve(fileClassCode);

        // then: THUMB와 ORIGINAL_SIZE만 순서대로 반환된다.
        assertThat(specs).extracting(VariantSpec::code)
            .containsExactly(FileVariantCode.THUMB, FileVariantCode.ORIGINAL_SIZE);
    }

    // 게시글 첨부파일은 변환본 정책을 생성하지 않아야 한다.
    @Test
    void resolve_article_attachment_returns_empty_policy() {
        // given: 게시글 첨부파일 클래스 코드
        String fileClassCode = FileClassCode.ARTICLE_ATTACHMENT;

        // when: 변환본 정책을 조회하면
        List<VariantSpec> specs = imageVariantPolicy.resolve(fileClassCode);

        // then: 변환본 정책을 반환하지 않는다.
        assertThat(specs).isEmpty();
    }

    // 게시글 썸네일 이미지는 THUMB와 LARGE 정책을 유지해야 한다.
    @Test
    void resolve_article_thumbnail_keeps_thumb_and_large() {
        // given: 게시글 썸네일 클래스 코드
        String fileClassCode = FileClassCode.ARTICLE_THUMBNAIL;

        // when: 변환본 정책을 조회하면
        List<VariantSpec> specs = imageVariantPolicy.resolve(fileClassCode);

        // then: THUMB와 LARGE를 반환한다.
        assertThat(specs).extracting(VariantSpec::code)
            .containsExactly(FileVariantCode.THUMB, FileVariantCode.LARGE);
    }
}
