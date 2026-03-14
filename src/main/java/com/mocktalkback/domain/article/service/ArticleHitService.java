package com.mocktalkback.domain.article.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleHitService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public long increaseAndGet(Long articleId) {
        String sql = """
            update tb_articles
            set hit = hit + 1
            where article_id = :articleId
              and deleted_at is null
            returning hit
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("articleId", articleId);
        Long nextHit = jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getLong("hit");
        });

        if (nextHit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "article not found");
        }
        return nextHit;
    }
}
