package com.mocktalkback.domain.newsbot.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NewsBotContentBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    public String build(NewsBotSourceItem item, String timezone) {
        StringBuilder builder = new StringBuilder();
        builder.append("> 뉴스봇이 외부 새소식을 자동 수집해 생성한 게시글입니다.\n\n");
        builder.append("## 원문 정보\n");
        builder.append("- 출처: ").append(fallback(item.sourceLabel(), "알 수 없음")).append('\n');
        if (StringUtils.hasText(item.authorName())) {
            builder.append("- 작성자: ").append(item.authorName().trim()).append('\n');
        }
        if (item.publishedAt() != null) {
            builder.append("- 발행 시각: ")
                .append(DATE_TIME_FORMATTER.format(item.publishedAt().atZone(ZoneId.of(timezone))))
                .append('\n');
        }
        builder.append("- 원문 링크: [바로가기](").append(item.externalUrl()).append(")\n");
        builder.append('\n');

        if (StringUtils.hasText(item.summary())) {
            builder.append("## 요약\n");
            builder.append(item.summary().trim()).append("\n\n");
        }

        builder.append("## 안내\n");
        builder.append("- 원문 링크와 함께 자동 생성된 글입니다.\n");
        builder.append("- 내용 정확도와 최신성은 원문을 우선 확인해 주세요.\n");
        return builder.toString();
    }

    private String fallback(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }
}
