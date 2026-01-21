package com.mocktalkback.domain.file.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemporaryFilePolicy {

    private final Duration expireDuration;

    public TemporaryFilePolicy(@Value("${app.file.temp-expire-hours:24}") long expireHours) {
        this.expireDuration = Duration.ofHours(Math.max(1L, expireHours));
    }

    public Instant resolveExpiry() {
        return resolveExpiry(Instant.now());
    }

    public Instant resolveExpiry(Instant baseTime) {
        if (baseTime == null) {
            return Instant.now().plus(expireDuration);
        }
        return baseTime.plus(expireDuration);
    }
}
