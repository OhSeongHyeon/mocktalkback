package com.mocktalkback.domain.newsbot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class NewsBotPayloadHasher {

    public String hash(NewsBotSourceItem item) {
        String payload = String.join(
            "||",
            nullSafe(item.externalItemKey()),
            nullSafe(item.title()),
            nullSafe(item.externalUrl()),
            nullSafe(item.summary()),
            nullSafe(item.sourceLabel()),
            nullSafe(item.authorName()),
            formatInstant(item.publishedAt()),
            formatInstant(item.sourceUpdatedAt())
        );

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("뉴스 payload 해시를 계산할 수 없습니다.", exception);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String formatInstant(Instant value) {
        return value == null ? "" : value.toString();
    }
}
