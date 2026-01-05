package com.mocktalkback.domain.user.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class HandleGenerator {

    private HandleGenerator() {}

    public static String baseFrom(String displayName, String userName) {
        String raw = (displayName != null && !displayName.isBlank()) ? displayName : userName;

        // 한글/특수문자 대비: 일단 영문/숫자만 남기고 나머지 제거(간단 버전)
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");

        if (normalized.isBlank()) {
            normalized = "user";
        }

        // 길이 제한(테이블 24)
        if (normalized.length() > 16) normalized = normalized.substring(0, 16);

        return normalized;
    }

    public static String randomSuffix() {
        // 5자리 숫자 suffix
        int n = ThreadLocalRandom.current().nextInt(10000, 100000);
        return String.valueOf(n);
    }
}
