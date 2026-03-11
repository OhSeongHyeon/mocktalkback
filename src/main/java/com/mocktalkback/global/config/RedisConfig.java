package com.mocktalkback.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.mocktalkback.domain.realtime.config.RealtimeRedisProperties;
import com.mocktalkback.domain.realtime.service.RealtimeRedisSubscriber;

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

    @Bean
    public RealtimeRedisProperties realtimeRedisProperties(
        @Value("${app.realtime.redis.enabled:false}") boolean enabled,
        @Value("${app.realtime.redis.fallback-enabled:true}") boolean fallbackEnabled,
        @Value("${app.realtime.redis.channels.notification:realtime:notification:events}") String notificationChannel,
        @Value("${app.realtime.redis.channels.board:realtime:board:events}") String boardChannel,
        @Value("${app.realtime.notification-ticket.ttl-seconds:30}") long notificationTicketTtlSeconds,
        @Value("${app.realtime.presence.ttl-seconds:45}") long presenceTtlSeconds,
        @Value("${app.realtime.presence.max-sessions:8}") int presenceMaxSessions
    ) {
        return new RealtimeRedisProperties(
            enabled,
            fallbackEnabled,
            notificationChannel,
            boardChannel,
            Duration.ofSeconds(notificationTicketTtlSeconds),
            Duration.ofSeconds(presenceTtlSeconds),
            presenceMaxSessions
        );
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory redisConnectionFactory,
        RealtimeRedisSubscriber realtimeRedisSubscriber,
        RealtimeRedisProperties realtimeRedisProperties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        if (!realtimeRedisProperties.enabled()) {
            return container;
        }
        container.addMessageListener(
            realtimeRedisSubscriber,
            ChannelTopic.of(realtimeRedisProperties.notificationChannel())
        );
        container.addMessageListener(
            realtimeRedisSubscriber,
            ChannelTopic.of(realtimeRedisProperties.boardChannel())
        );
        return container;
    }
}
