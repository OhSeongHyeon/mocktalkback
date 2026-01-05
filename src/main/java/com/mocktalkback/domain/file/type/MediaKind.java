package com.mocktalkback.domain.file.type;

public enum MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,  // pdf, 오피스, 텍스트 등 문서파일
    ARCHIVE,  // zip, 7z 등 압축파일
    OTHER,  // 예외, 미분류
    ANY
}

