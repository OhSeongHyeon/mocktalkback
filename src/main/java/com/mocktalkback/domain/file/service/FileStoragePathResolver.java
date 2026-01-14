package com.mocktalkback.domain.file.service;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.file.type.FileClassCode;

@Component
public class FileStoragePathResolver {

    public String resolveCategory(String fileClassCode) {
        if (!StringUtils.hasText(fileClassCode)) {
            throw new IllegalArgumentException("파일 분류 코드가 비어있습니다.");
        }
        if (FileClassCode.PROFILE_IMAGE.equals(fileClassCode)) {
            // 기존 프로필 이미지는 profile 경로를 유지합니다.
            return "profile";
        }
        return fileClassCode.toLowerCase(Locale.ROOT);
    }
}
