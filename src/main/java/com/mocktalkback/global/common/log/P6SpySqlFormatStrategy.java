package com.mocktalkback.global.common.log;

import org.hibernate.engine.jdbc.internal.FormatStyle;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

public class P6SpySqlFormatStrategy implements MessageFormattingStrategy {

    @Override
    public String formatMessage(
        int connectionId,
        String now,
        long elapsed,
        String category,
        String prepared,
        String sql,
        String url
    ) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        String formattedSql;

        try {
            formattedSql = FormatStyle.BASIC.getFormatter().format(sql);
        } catch (Exception e) {
            formattedSql = sql;  // fallback
        }
        // Hibernate 포맷터로 SQL 줄바꿈을 적용해 가독성을 높인다.
        return "[" + category + "] " + elapsed + "ms" + System.lineSeparator() + formattedSql;
    }
}
