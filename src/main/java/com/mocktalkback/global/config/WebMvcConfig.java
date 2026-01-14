package com.mocktalkback.global.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile("dev")
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String storageDir;

    public WebMvcConfig(@Value("${app.file.storage-dir:uploads}") String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = resolveStorageLocation(storageDir);
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(location);
    }

    private String resolveStorageLocation(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            trimmed = "uploads";
        }
        Path path = Paths.get(trimmed).toAbsolutePath().normalize();
        return "file:" + path + "/";
    }
}
