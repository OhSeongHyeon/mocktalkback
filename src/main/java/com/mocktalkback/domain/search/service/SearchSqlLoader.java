package com.mocktalkback.domain.search.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SearchSqlLoader {

    private final Map<SearchSqlId, String> cache = new ConcurrentHashMap<>();

    public String getSql(SearchSqlId sqlId) {
        return cache.computeIfAbsent(sqlId, this::loadSql);
    }

    private String loadSql(SearchSqlId sqlId) {
        ClassPathResource resource = new ClassPathResource(sqlId.resourcePath());
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String sql = new String(bytes, StandardCharsets.UTF_8).trim();
            if (sql.isEmpty()) {
                throw new IllegalStateException("빈 SQL 파일입니다: " + sqlId.resourcePath());
            }
            return sql;
        } catch (IOException exception) {
            throw new IllegalStateException("SQL 파일을 로드할 수 없습니다: " + sqlId.resourcePath(), exception);
        }
    }
}
