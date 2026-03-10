package com.mocktalkback.domain.article.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Component
public class ArticleImportBundleParser {

    private static final Set<String> MARKDOWN_EXTENSIONS = Set.of(".md", ".markdown");
    private static final Set<String> YAML_EXTENSIONS = Set.of(".yml", ".yaml");
    private final Yaml yaml;

    public ArticleImportBundleParser() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new SafeConstructor(loaderOptions));
    }

    public ArticleImportBundle parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("zip 파일이 비어 있습니다.");
        }

        Map<String, String> textEntries = readTextEntries(file);
        String manifestPath = resolveManifestPath(textEntries.keySet());
        Map<String, Object> manifest = asMap(yaml.load(textEntries.get(manifestPath)), "manifest 형식이 올바르지 않습니다.");
        Map<String, Object> defaults = getMap(manifest, "defaults");
        List<?> articles = getList(manifest.get("articles"), "manifest articles는 배열이어야 합니다.");
        if (articles.isEmpty()) {
            throw new IllegalArgumentException("manifest에 articles 항목이 없습니다.");
        }

        String manifestDirectory = extractDirectory(manifestPath);
        List<ArticleImportCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < articles.size(); index += 1) {
            Object rawItem = articles.get(index);
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                candidates.add(new ArticleImportCandidate(
                    "articles[" + index + "]",
                    null,
                    null,
                    null,
                    "",
                    List.of(),
                    List.of("articles[" + index + "] 항목 형식이 올바르지 않습니다.")
                ));
                continue;
            }

            Map<String, Object> item = castMap(rawMap);
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            String filePath = normalizeZipPath(readString(item, "file"));
            if (!StringUtils.hasText(filePath)) {
                errors.add("markdown 파일 경로(file)가 없습니다.");
                candidates.add(new ArticleImportCandidate(
                    "articles[" + index + "]",
                    normalizeText(resolveString(item, "title")),
                    normalizeText(resolveString(item, "boardSlug", "board_slug", "board-slug")),
                    normalizeText(resolveString(item, "visibility")),
                    "",
                    warnings,
                    errors
                ));
                continue;
            }

            String directPath = normalizeZipPath(filePath);
            String resolvedPath = textEntries.containsKey(directPath)
                ? directPath
                : resolveRelativePath(manifestDirectory, filePath);
            String markdown = textEntries.get(resolvedPath);
            if (!StringUtils.hasText(markdown)) {
                errors.add("markdown 파일을 찾을 수 없습니다: " + filePath);
                candidates.add(new ArticleImportCandidate(
                    filePath,
                    normalizeText(resolveString(item, "title")),
                    normalizeText(resolveString(item, "boardSlug", "board_slug", "board-slug")),
                    normalizeText(resolveString(item, "visibility")),
                    "",
                    warnings,
                    errors
                ));
                continue;
            }

            FrontmatterResult frontmatter = parseFrontmatter(markdown);
            warnings.addAll(frontmatter.warnings());
            errors.addAll(frontmatter.errors());

            String title = firstNonBlank(
                resolveString(item, "title"),
                frontmatter.metadata().title(),
                deriveTitleFromPath(filePath)
            );
            String boardSlug = firstNonBlank(
                resolveString(item, "boardSlug", "board_slug", "board-slug"),
                frontmatter.metadata().boardSlug(),
                resolveString(defaults, "boardSlug", "board_slug", "board-slug")
            );
            String visibility = firstNonBlank(
                resolveString(item, "visibility"),
                frontmatter.metadata().visibility(),
                resolveString(defaults, "visibility"),
                "PUBLIC"
            );

            if (!frontmatter.metadata().tags().isEmpty()) {
                warnings.add("frontmatter tags는 아직 자동 반영되지 않아 무시됩니다.");
            }
            if (StringUtils.hasText(frontmatter.metadata().summary())) {
                warnings.add("frontmatter summary는 아직 자동 반영되지 않아 무시됩니다.");
            }

            candidates.add(new ArticleImportCandidate(
                filePath,
                normalizeText(title),
                normalizeText(boardSlug),
                normalizeText(visibility),
                frontmatter.content(),
                warnings,
                errors
            ));
        }

        return new ArticleImportBundle(file.getOriginalFilename(), candidates);
    }

    private Map<String, String> readTextEntries(MultipartFile file) {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String normalizedPath = normalizeZipPath(entry.getName());
                if (!isSupportedTextEntry(normalizedPath)) {
                    continue;
                }
                entries.put(normalizedPath, readEntry(zipInputStream));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("zip 파일을 읽을 수 없습니다.", exception);
        }
        return entries;
    }

    private String readEntry(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private boolean isSupportedTextEntry(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);
        return MARKDOWN_EXTENSIONS.stream().anyMatch(lowerCasePath::endsWith)
            || YAML_EXTENSIONS.stream().anyMatch(lowerCasePath::endsWith);
    }

    private String resolveManifestPath(Set<String> paths) {
        List<String> candidates = paths.stream()
            .filter(path -> path.endsWith("/manifest.yml")
                || path.endsWith("/manifest.yaml")
                || "manifest.yml".equals(path)
                || "manifest.yaml".equals(path))
            .sorted()
            .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("zip 안에 manifest.yml 또는 manifest.yaml 파일이 필요합니다.");
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("manifest 파일은 하나만 허용됩니다.");
        }
        return candidates.get(0);
    }

    private FrontmatterResult parseFrontmatter(String rawContent) {
        String content = stripBom(rawContent);
        if (!content.startsWith("---")) {
            return new FrontmatterResult(content, ArticleFrontmatterMetadata.empty(), List.of(), List.of());
        }

        String[] lines = content.split("\\R", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) {
            return new FrontmatterResult(content, ArticleFrontmatterMetadata.empty(), List.of(), List.of());
        }

        int closingIndex = -1;
        for (int index = 1; index < lines.length; index += 1) {
            if ("---".equals(lines[index].trim())) {
                closingIndex = index;
                break;
            }
        }

        if (closingIndex < 0) {
            return new FrontmatterResult(
                content,
                ArticleFrontmatterMetadata.empty(),
                List.of("frontmatter 종료 구분자가 없어 본문 전체를 Markdown으로 사용합니다."),
                List.of()
            );
        }

        String yamlBlock = String.join("\n", List.of(lines).subList(1, closingIndex));
        String markdownBody = String.join("\n", List.of(lines).subList(closingIndex + 1, lines.length));
        Object loaded;
        try {
            loaded = yaml.load(yamlBlock);
        } catch (RuntimeException exception) {
            return new FrontmatterResult(
                content,
                ArticleFrontmatterMetadata.empty(),
                List.of("frontmatter 파싱에 실패해 본문 전체를 Markdown으로 사용합니다."),
                List.of()
            );
        }

        if (loaded == null) {
            return new FrontmatterResult(markdownBody, ArticleFrontmatterMetadata.empty(), List.of(), List.of());
        }
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            return new FrontmatterResult(
                content,
                ArticleFrontmatterMetadata.empty(),
                List.of("frontmatter 형식이 올바르지 않아 본문 전체를 Markdown으로 사용합니다."),
                List.of()
            );
        }

        Map<String, Object> metadataMap = castMap(rawMap);
        ArticleFrontmatterMetadata metadata = new ArticleFrontmatterMetadata(
            normalizeText(resolveString(metadataMap, "title")),
            resolveStringList(metadataMap, "tags"),
            normalizeText(resolveString(metadataMap, "boardSlug", "board_slug", "board-slug")),
            normalizeText(resolveString(metadataMap, "visibility")),
            normalizeText(resolveString(metadataMap, "summary"))
        );
        return new FrontmatterResult(markdownBody, metadata, List.of(), List.of());
    }

    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return Map.of();
        }
        return asMap(value, key + " 형식이 올바르지 않습니다.");
    }

    private List<?> getList(Object value, String message) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException(message);
    }

    private Map<String, Object> asMap(Object value, String message) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException(message);
        }
        return castMap(rawMap);
    }

    private Map<String, Object> castMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = Objects.toString(entry.getKey(), "").trim();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private String resolveString(Map<String, Object> source, String... candidateKeys) {
        for (String candidateKey : candidateKeys) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (canonicalizeKey(entry.getKey()).equals(canonicalizeKey(candidateKey))) {
                    return entry.getValue() == null ? null : String.valueOf(entry.getValue()).trim();
                }
            }
        }
        return null;
    }

    private String readString(Map<String, Object> source, String... candidateKeys) {
        return normalizeText(resolveString(source, candidateKeys));
    }

    private List<String> resolveStringList(Map<String, Object> source, String... candidateKeys) {
        for (String candidateKey : candidateKeys) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (!canonicalizeKey(entry.getKey()).equals(canonicalizeKey(candidateKey))) {
                    continue;
                }
                Object value = entry.getValue();
                if (value instanceof Iterable<?> iterable) {
                    List<String> result = new ArrayList<>();
                    for (Object item : iterable) {
                        String normalized = normalizeText(item == null ? null : String.valueOf(item));
                        if (StringUtils.hasText(normalized)) {
                            result.add(normalized);
                        }
                    }
                    return result;
                }
                String normalized = normalizeText(value == null ? null : String.valueOf(value));
                return StringUtils.hasText(normalized) ? List.of(normalized) : List.of();
            }
        }
        return List.of();
    }

    private String canonicalizeKey(String key) {
        return key == null ? "" : key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private String extractDirectory(String path) {
        int separatorIndex = path.lastIndexOf('/');
        if (separatorIndex < 0) {
            return "";
        }
        return path.substring(0, separatorIndex);
    }

    private String resolveRelativePath(String baseDirectory, String relativePath) {
        if (!StringUtils.hasText(baseDirectory)) {
            return normalizeZipPath(relativePath);
        }
        return normalizeZipPath(baseDirectory + "/" + relativePath);
    }

    private String normalizeZipPath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return "";
        }
        String normalized = rawPath.replace('\\', '/').trim();
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : normalized.split("/")) {
            if (!StringUtils.hasText(segment) || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new IllegalArgumentException("zip 경로가 올바르지 않습니다: " + rawPath);
                }
                segments.removeLast();
                continue;
            }
            segments.addLast(segment);
        }
        return String.join("/", segments);
    }

    private String deriveTitleFromPath(String filePath) {
        String normalized = normalizeZipPath(filePath);
        int separatorIndex = normalized.lastIndexOf('/');
        String fileName = separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return normalizeText(baseName.replace('-', ' ').replace('_', ' '));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ArticleImportBundle(
        String sourceFileName,
        List<ArticleImportCandidate> articles
    ) {
    }

    public record ArticleImportCandidate(
        String filePath,
        String title,
        String boardSlug,
        String visibility,
        String contentSource,
        List<String> warnings,
        List<String> errors
    ) {
    }

    private record FrontmatterResult(
        String content,
        ArticleFrontmatterMetadata metadata,
        List<String> warnings,
        List<String> errors
    ) {
    }

    private record ArticleFrontmatterMetadata(
        String title,
        List<String> tags,
        String boardSlug,
        String visibility,
        String summary
    ) {
        private static ArticleFrontmatterMetadata empty() {
            return new ArticleFrontmatterMetadata(null, List.of(), null, null, null);
        }
    }
}
