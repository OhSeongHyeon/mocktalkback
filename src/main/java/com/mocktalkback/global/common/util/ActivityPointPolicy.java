package com.mocktalkback.global.common.util;

/**
 * 활동 포인트 지급/차감 정책
 */
public enum ActivityPointPolicy {

    JOIN(20_000),
    CREATE_BOARD(-20_000),
    CREATE_ARTICLE(200),
    DELETE_ARTICLE(-200),
    CREATE_REPLY(40),
    DELETE_REPLY(-40);

    public final int delta;

    ActivityPointPolicy(int delta) { this.delta = delta; }

}

