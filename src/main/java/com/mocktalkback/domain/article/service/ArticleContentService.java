package com.mocktalkback.domain.article.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.global.common.sanitize.HtmlSanitizer;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Service
public class ArticleContentService {

    private static final Pattern TABLE_DELIMITER_PATTERN = Pattern.compile(
        "^\\s*\\|?(\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$"
    );
    private final HtmlSanitizer htmlSanitizer;
    private final Parser markdownParser;
    private final HtmlRenderer markdownRenderer;

    public ArticleContentService(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            AutolinkExtension.create()
        ));

        this.markdownParser = Parser.builder(options).build();
        this.markdownRenderer = HtmlRenderer.builder(options).build();
    }

    public RenderedContent render(String contentSource, ArticleContentFormat contentFormat) {
        if (contentFormat == null) {
            throw new IllegalArgumentException("contentFormat은 필수입니다.");
        }
        if (contentSource == null) {
            throw new IllegalArgumentException("contentSource는 필수입니다.");
        }

        if (contentFormat == ArticleContentFormat.HTML) {
            String sanitized = htmlSanitizer.sanitize(contentSource);
            return new RenderedContent(sanitized, sanitized);
        }

        String normalizedMarkdown = normalizeMarkdownTables(contentSource);
        String rendered = markdownRenderer.render(markdownParser.parse(normalizedMarkdown));
        String sanitized = htmlSanitizer.sanitize(rendered);
        return new RenderedContent(sanitized, contentSource);
    }

    public record RenderedContent(String content, String contentSource) {
    }

    private String normalizeMarkdownTables(String markdown) {
        if (markdown.isBlank()) {
            return markdown;
        }

        List<String> lines = List.of(markdown.split("\\R", -1));
        List<String> normalized = new ArrayList<>();
        boolean inCodeFence = false;

        for (int index = 0; index < lines.size(); index += 1) {
            String line = lines.get(index);

            if (isCodeFenceLine(line)) {
                inCodeFence = !inCodeFence;
                normalized.add(line);
                continue;
            }

            if (!inCodeFence && isTableHeaderLine(line) && index + 1 < lines.size() && isTableDelimiterLine(lines.get(index + 1))) {
                if (!normalized.isEmpty() && !normalized.get(normalized.size() - 1).isBlank()) {
                    normalized.add("");
                }

                normalized.add(line);
                normalized.add(lines.get(index + 1));
                index += 2;

                while (index < lines.size() && isTableRowLine(lines.get(index))) {
                    normalized.add(lines.get(index));
                    index += 1;
                }

                if (index < lines.size() && !lines.get(index).isBlank() && !normalized.get(normalized.size() - 1).isBlank()) {
                    normalized.add("");
                }

                index -= 1;
                continue;
            }

            normalized.add(line);
        }

        return String.join("\n", normalized);
    }

    private boolean isCodeFenceLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("```") || trimmed.startsWith("~~~");
    }

    private boolean isTableHeaderLine(String line) {
        String trimmed = line.trim();
        return !trimmed.isEmpty() && trimmed.contains("|") && !TABLE_DELIMITER_PATTERN.matcher(trimmed).matches();
    }

    private boolean isTableDelimiterLine(String line) {
        return TABLE_DELIMITER_PATTERN.matcher(line.trim()).matches();
    }

    private boolean isTableRowLine(String line) {
        String trimmed = line.trim();
        return !trimmed.isEmpty() && trimmed.contains("|") && !isCodeFenceLine(trimmed);
    }
}
