package com.mocktalkback.domain.file.upload.type;

import com.mocktalkback.domain.file.type.FileClassCode;

public enum UploadPurpose {
    EDITOR_IMAGE,
    EDITOR_VIDEO,
    ARTICLE_ATTACHMENT,
    BOARD_IMAGE,
    PROFILE_IMAGE;

    public String toFileClassCode() {
        if (this == EDITOR_IMAGE) {
            return FileClassCode.ARTICLE_CONTENT_IMAGE;
        }
        if (this == EDITOR_VIDEO) {
            return FileClassCode.ARTICLE_CONTENT_VIDEO;
        }
        if (this == ARTICLE_ATTACHMENT) {
            return FileClassCode.ARTICLE_ATTACHMENT;
        }
        if (this == BOARD_IMAGE) {
            return FileClassCode.BOARD_IMAGE;
        }
        return FileClassCode.PROFILE_IMAGE;
    }
}
