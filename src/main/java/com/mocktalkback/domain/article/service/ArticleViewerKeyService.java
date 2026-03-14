package com.mocktalkback.domain.article.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleViewerKeyService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_USER_AGENT_LENGTH = 200;

    @Value("${app.article.view.anon-hash-secret:mocktalk-article-view-anon-secret}")
    private String anonHashSecret;

    public String resolve(Long userId, String clientIp, String userAgent) {
        if (userId != null) {
            return "user:" + userId;
        }
        String material = normalizeClientIp(clientIp) + "|" + normalizeUserAgent(userAgent);
        return "anon:" + hmacSha256(material);
    }

    private String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private String normalizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }

        String normalized = userAgent.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);

        if (normalized.length() > MAX_USER_AGENT_LENGTH) {
            return normalized.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return normalized;
    }

    private String hmacSha256(String material) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(anonHashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("게시글 조회 익명 식별 키 생성에 실패했습니다.", ex);
        }
    }
}
