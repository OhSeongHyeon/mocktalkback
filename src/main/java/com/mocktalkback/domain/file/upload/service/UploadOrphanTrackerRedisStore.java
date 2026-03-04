package com.mocktalkback.domain.file.upload.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UploadOrphanTrackerRedisStore {

    private static final String ORPHAN_ZSET_KEY = "upload:orphan:deadline";
    private static final String TOKEN_META_PREFIX = "upload:orphan:token:";

    private final StringRedisTemplate stringRedisTemplate;

    public UploadOrphanTrackerRedisStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void track(String uploadToken, String storageKey, long deadlineEpochSeconds) {
        if (!StringUtils.hasText(uploadToken) || !StringUtils.hasText(storageKey)) {
            return;
        }
        String token = uploadToken.trim();
        stringRedisTemplate.opsForValue().set(tokenMetaKey(token), storageKey.trim());
        stringRedisTemplate.opsForZSet().add(ORPHAN_ZSET_KEY, token, deadlineEpochSeconds);
    }

    public void untrack(String uploadToken) {
        if (!StringUtils.hasText(uploadToken)) {
            return;
        }
        String token = uploadToken.trim();
        stringRedisTemplate.opsForZSet().remove(ORPHAN_ZSET_KEY, token);
        stringRedisTemplate.delete(tokenMetaKey(token));
    }

    public List<String> popExpiredTokens(long nowEpochSeconds, int batchSize) {
        if (batchSize <= 0) {
            return List.of();
        }
        Set<String> tokens = stringRedisTemplate.opsForZSet().rangeByScore(ORPHAN_ZSET_KEY, 0, nowEpochSeconds, 0, batchSize);
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            Long removed = stringRedisTemplate.opsForZSet().remove(ORPHAN_ZSET_KEY, token);
            if (removed == null || removed <= 0L) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    public String getTrackedStorageKey(String uploadToken) {
        if (!StringUtils.hasText(uploadToken)) {
            return null;
        }
        return stringRedisTemplate.opsForValue().get(tokenMetaKey(uploadToken.trim()));
    }

    public void deleteTokenMeta(String uploadToken) {
        if (!StringUtils.hasText(uploadToken)) {
            return;
        }
        stringRedisTemplate.delete(tokenMetaKey(uploadToken.trim()));
    }

    private String tokenMetaKey(String uploadToken) {
        return TOKEN_META_PREFIX + uploadToken;
    }
}
