package com.mocktalkback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(
    exclude = RedisRepositoriesAutoConfiguration.class  // 레디스레포스캔은 RedisRepositoryConfig 에서함, jpa 레포스캔로그방지
)
@ConfigurationPropertiesScan
public class MocktalkbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MocktalkbackApplication.class, args);
    }
}
