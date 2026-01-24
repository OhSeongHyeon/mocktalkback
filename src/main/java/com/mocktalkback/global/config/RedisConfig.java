package com.mocktalkback.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    private RedisStandaloneConfiguration baseRedisConfig() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        if (password != null && !password.isBlank()) { redisConfig.setPassword(RedisPassword.of(password)); }
        return redisConfig;
    }

    @Bean
    @Profile("!prod")
    public RedisConnectionFactory redisConnectionFactoryNoSsl() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .build();
        return new LettuceConnectionFactory(baseRedisConfig(), clientConfig);
    }

    @Bean
    @Profile("prod")
    public RedisConnectionFactory redisConnectionFactoryWithSsl() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .build();
        return new LettuceConnectionFactory(baseRedisConfig(), clientConfig);
    }
}
