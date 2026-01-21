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
        if (isArticleImage(fileClassCode)) {
            return List.of(
                new VariantSpec(FileVariantCode.THUMB, 480),
                new VariantSpec(FileVariantCode.LARGE, 1600)
            );
        }
        return List.of();
    }

    private boolean isArticleImage(String fileClassCode) {
        return FileClassCode.ARTICLE_CONTENT_IMAGE.equals(fileClassCode)
            || FileClassCode.ARTICLE_ATTACHMENT.equals(fileClassCode)
            || FileClassCode.ARTICLE_THUMBNAIL.equals(fileClassCode);
    }

    public record VariantSpec(
        FileVariantCode code,
        int maxSize
    ) {
    }
}
