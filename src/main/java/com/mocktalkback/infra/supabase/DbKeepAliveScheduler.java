package com.mocktalkback.infra.supabase;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class DbKeepAliveScheduler {

    private final JdbcTemplate jdbcTemplate;

    // 하루 1번, supabase 유휴길면 sleep 함, sleep 방지용
    @Scheduled(cron = "0 0 3 * * *")
    public void keepAlive() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Supabase DB keep-alive OK");
        } catch (Exception e) {
            log.info("Supabase DB keep-alive failed: {}", e.getMessage());
        }
    }
}
