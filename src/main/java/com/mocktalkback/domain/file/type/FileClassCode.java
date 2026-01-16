package com.mocktalkback.domain.file.type;

public final class FileClassCode {
    private FileClassCode() {}

    // USER
    public static final String PROFILE_IMAGE = "PROFILE_IMAGE";
    public static final String PROFILE_BANNER = "PROFILE_BANNER";          // 프로필 상단 배너/커버
    public static final String AVATAR_IMAGE = "AVATAR_IMAGE";              // 썸네일 전용(리사이즈된 아바타)
    // public static final String USER_VERIFICATION = "USER_VERIFICATION";    // 본인인증/서류

    // ARTICLE (게시글)
    public static final String ARTICLE_ATTACHMENT = "ARTICLE_ATTACHMENT";  // 일반 첨부(이미지/비디오/파일)
    public static final String ARTICLE_THUMBNAIL = "ARTICLE_THUMBNAIL";    // 대표/썸네일
    public static final String ARTICLE_CONTENT_IMAGE = "ARTICLE_CONTENT_IMAGE"; // 본문 이미지(에디터 업로드)
    public static final String ARTICLE_CONTENT_VIDEO = "ARTICLE_CONTENT_VIDEO"; // 본문 비디오

    // BOARD (게시판)
    public static final String BOARD_IMAGE = "BOARD_IMAGE"; // 게시판 대표 이미지

    // COMMENT (댓글) - 있으면
    public static final String COMMENT_ATTACHMENT = "COMMENT_ATTACHMENT";

    // GALLERY (이미지 갤러리 도메인이 따로 있으면)
    public static final String GALLERY_ITEM_IMAGE = "GALLERY_ITEM_IMAGE";  // 갤러리 아이템 원본
    public static final String GALLERY_ITEM_THUMBNAIL = "GALLERY_ITEM_THUMBNAIL"; // 갤러리 썸네일

    // ADMIN / SITE
    public static final String SITE_BANNER = "SITE_BANNER";
    public static final String SITE_POPUP = "SITE_POPUP";
}

