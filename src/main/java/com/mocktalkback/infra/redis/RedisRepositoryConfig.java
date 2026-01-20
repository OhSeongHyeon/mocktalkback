package com.mocktalkback.infra.redis;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * 레디스 레포지토리 스캔 범위를 명확히 지정합니다.
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.mocktalkback.infra.redis")
public class RedisRepositoryConfig {
}
