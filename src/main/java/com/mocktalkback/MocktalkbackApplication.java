package com.mocktalkback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MocktalkbackApplication {

    public static void main(String[] args) {
        String profile = System.getProperty(
            "spring.profiles.active", 
            System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev")
        );

        if ("dev".equals(profile)) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        }

        SpringApplication.run(MocktalkbackApplication.class, args);
    }


}
