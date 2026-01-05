package com.mocktalkback.domain.role.type;

public final class AuthBits {
    private AuthBits() {}

    public static final int READ   = 1;       // 0001
    public static final int WRITE  = 1 << 1;  // 0010
    public static final int DELETE = 1 << 2;  // 0100
    public static final int ADMIN  = 1 << 3;  // 1000
}
