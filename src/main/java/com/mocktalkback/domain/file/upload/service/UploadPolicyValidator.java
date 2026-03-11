package com.mocktalkback.domain.file.upload.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.file.upload.dto.UploadInitContext;
import com.mocktalkback.domain.file.upload.type.UploadPurpose;

@Component
public class UploadPolicyValidator {

    private static final long MAX_UPLOAD_SIZE = 50L * 1024L * 1024L;

    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "hwp", "hwpx",
        "txt", "csv", "zip", "7z", "rar",
        "jpg", "jpeg", "png", "webp", "gif",
        "mp4", "webm", "mp3", "wav"
    );

    private static final Set<String> ALLOWED_ATTACHMENT_MIME_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/x-hwp",
        "application/vnd.hancom.hwp",
        "application/haansofthwp",
        "application/haansofthwpx",
        "application/vnd.hancom.hwpx",
        "text/plain",
        "text/csv",
        "application/csv",
        "application/zip",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-rar-compressed",
        "application/vnd.rar",
        "image/jpg",
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif",
        "video/mp4",
        "video/webm",
        "audio/mpeg",
        "audio/wav",
        "audio/x-wav"
    );

    private static final Set<String> BLOCKED_ATTACHMENT_EXTENSIONS = Set.of(
        "ade", "adp", "apk", "appx", "bat", "cmd", "com", "cpl", "dll", "exe", "hta",
        "ins", "isp", "jar", "js", "jse", "lnk", "msc", "msi", "msp", "mst", "pif",
        "ps1", "reg", "scr", "sh", "vb", "vbe", "vbs", "ws", "wsc", "wsf", "wsh"
    );

    private static final Set<String> BLOCKED_ATTACHMENT_MIME_TYPES = Set.of(
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-dosexec",
        "application/x-sh",
        "application/x-bat",
        "application/x-shellscript"
    );

    public void validateInit(
        UploadPurpose purpose,
        String originalFileName,
        String contentType,
        long fileSize,
        UploadInitContext context
    ) {
        if (purpose == null) {
            throw new IllegalArgumentException("업로드 목적이 비어있습니다.");
        }
        if (!StringUtils.hasText(originalFileName)) {
            throw new IllegalArgumentException("원본 파일명이 비어있습니다.");
        }
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("파일 형식 정보가 비어있습니다.");
        }
        if (fileSize <= 0L) {
            throw new IllegalArgumentException("파일 크기가 올바르지 않습니다.");
        }
        if (fileSize > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("파일 사이즈 제한 50MB");
        }

        String normalizedMimeType = normalizeMimeType(contentType);
        if (purpose == UploadPurpose.EDITOR_IMAGE) {
            validateImageMimeType(normalizedMimeType);
            return;
        }
        if (purpose == UploadPurpose.EDITOR_VIDEO) {
            validateEditorVideoMimeType(normalizedMimeType);
            return;
        }
        if (purpose == UploadPurpose.ARTICLE_ATTACHMENT) {
            validateAttachment(originalFileName, normalizedMimeType);
            return;
        }
        if (purpose == UploadPurpose.BOARD_IMAGE) {
            validateBoardContext(context);
            validateImageMimeType(normalizedMimeType);
            return;
        }
        validateImageMimeType(normalizedMimeType);
    }

    public String normalizeMimeType(String contentType) {
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex).trim();
        }
        return normalized;
    }

    private void validateBoardContext(UploadInitContext context) {
        if (context == null) {
            throw new IllegalArgumentException("게시판 이미지 업로드 컨텍스트가 비어있습니다.");
        }
        if (context.boardId() == null || context.boardId() <= 0L) {
            throw new IllegalArgumentException("게시판 식별자가 올바르지 않습니다.");
        }
        if (context.channel() == null) {
            throw new IllegalArgumentException("게시판 이미지 업로드 채널이 비어있습니다.");
        }
    }

    private void validateAttachment(String originalFileName, String normalizedMimeType) {
        String extension = normalizeExtension(resolveExtension(originalFileName));
        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
        if (BLOCKED_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("업로드할 수 없는 확장자입니다.");
        }
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }

        if (BLOCKED_ATTACHMENT_MIME_TYPES.contains(normalizedMimeType)) {
            throw new IllegalArgumentException("업로드할 수 없는 파일 형식입니다.");
        }
        if ("application/octet-stream".equals(normalizedMimeType)) {
            return;
        }
        if (!ALLOWED_ATTACHMENT_MIME_TYPES.contains(normalizedMimeType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
    }

    private void validateImageMimeType(String normalizedMimeType) {
        if (!normalizedMimeType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private void validateEditorVideoMimeType(String normalizedMimeType) {
        if (!"video/mp4".equals(normalizedMimeType)
            && !"video/webm".equals(normalizedMimeType)
            && !"video/ogg".equals(normalizedMimeType)) {
            throw new IllegalArgumentException("MP4, WebM 또는 Ogg 영상만 업로드할 수 있습니다.");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        String normalized = originalFilename.trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(dotIndex + 1);
    }

    private String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return null;
        }
        return extension.trim().toLowerCase(Locale.ROOT);
    }
}
