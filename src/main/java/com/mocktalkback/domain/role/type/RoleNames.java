package com.mocktalkback.domain.role.type;

public final class RoleNames {
    private RoleNames() {}

    public static final String USER = "USER";  // 기본 사용자(읽기), 0001
    public static final String WRITER = "WRITER";  // 작성 가능(읽기+쓰기), 0010
    public static final String MANAGER = "MANAGER";  // 매니저(읽기+쓰기+삭제), 0100
    public static final String ADMIN = "ADMIN";  // 전체 권한, 1000
}

