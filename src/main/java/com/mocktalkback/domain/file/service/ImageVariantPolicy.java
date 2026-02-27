package com.mocktalkback.domain.file.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.FileVariantCode;

@Component
public class ImageVariantPolicy {

    public List<VariantSpec> resolve(String fileClassCode) {
        if (FileClassCode.PROFILE_IMAGE.equals(fileClassCode)) {
            return List.of(
                new VariantSpec(FileVariantCode.MEDIUM, 256)
            );
        }
        if (FileClassCode.BOARD_IMAGE.equals(fileClassCode)) {
            return List.of(
                new VariantSpec(FileVariantCode.THUMB, 320),
                new VariantSpec(FileVariantCode.MEDIUM, 960)
            );
        }
        if (isArticleContentImage(fileClassCode)) {
            return List.of(
                new VariantSpec(FileVariantCode.THUMB, 480),
                // ORIGINAL_SIZE는 원본 해상도를 그대로 유지하는 정책 변환본이다.
                new VariantSpec(FileVariantCode.ORIGINAL_SIZE, 0)
            );
        }
        if (isArticleSupportImage(fileClassCode)) {
            return List.of(
                new VariantSpec(FileVariantCode.THUMB, 480),
                new VariantSpec(FileVariantCode.LARGE, 1600)
            );
        }
        return List.of();
    }

    private boolean isArticleContentImage(String fileClassCode) {
        return FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode);
    }

    private boolean isArticleSupportImage(String fileClassCode) {
        return FileClassCode.ARTICLE_THUMBNAIL.equals(fileClassCode);
    }

    public record VariantSpec(
        FileVariantCode code,
        int maxSize
    ) {
    }
}
