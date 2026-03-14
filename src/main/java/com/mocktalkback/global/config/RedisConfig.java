package com.mocktalkback.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;

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

    @Value("${app.redis.client.command-timeout-ms:500}")
    private long commandTimeoutMs;

    @Value("${app.redis.client.connect-timeout-ms:1000}")
    private long connectTimeoutMs;

    @Value("${app.redis.client.shutdown-timeout-ms:100}")
    private long shutdownTimeoutMs;

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
        return new LettuceConnectionFactory(baseRedisConfig(), buildClientConfig(false));
    }

    @Bean
    @Profile("prod")
    public RedisConnectionFactory redisConnectionFactoryWithSsl() {
        return new LettuceConnectionFactory(baseRedisConfig(), buildClientConfig(true));
    }

    private LettuceClientConfiguration buildClientConfig(boolean useSsl) {
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(Math.max(1L, connectTimeoutMs)))
            .build();

        ClientOptions clientOptions = ClientOptions.builder()
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .socketOptions(socketOptions)
            .timeoutOptions(TimeoutOptions.enabled())
            .build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(Math.max(1L, commandTimeoutMs)))
            .shutdownTimeout(Duration.ofMillis(Math.max(0L, shutdownTimeoutMs)))
            .clientOptions(clientOptions);

        if (useSsl) {
            builder.useSsl();
        }

        return builder.build();
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
