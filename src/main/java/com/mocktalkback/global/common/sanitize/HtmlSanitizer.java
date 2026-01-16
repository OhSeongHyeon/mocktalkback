package com.mocktalkback.global.common.sanitize;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class HtmlSanitizer {

    private static final List<String> DEFAULT_IFRAME_PREFIXES = List.of(
        "https://www.youtube.com/embed",
        "https://www.youtube-nocookie.com/embed"
    );

    private final HtmlSanitizerProperties properties;
    private PolicyFactory policy;
    private Set<String> allowedFileOrigins;
    private Set<String> allowedIframePrefixes;

    public HtmlSanitizer(HtmlSanitizerProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        this.allowedFileOrigins = normalizeOrigins(properties.getAllowedFileOrigins());
        Set<String> iframeOrigins = normalizeOrigins(properties.getAllowedIframePrefixes());
        this.allowedIframePrefixes = iframeOrigins.isEmpty()
            ? DEFAULT_IFRAME_PREFIXES.stream().collect(Collectors.toSet())
            : iframeOrigins;
        this.policy = buildPolicy();
    }

    public String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return policy.sanitize(html);
    }

    private PolicyFactory buildPolicy() {
        AttributePolicy mediaSrcPolicy = (elementName, attributeName, value) -> {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return isAllowedMediaSrc(trimmed) ? trimmed : null;
        };

        AttributePolicy iframeSrcPolicy = (elementName, attributeName, value) -> {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return isAllowedIframeSrc(trimmed) ? trimmed : null;
        };

        AttributePolicy linkHrefPolicy = (elementName, attributeName, value) -> {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return isAllowedLinkHref(trimmed) ? trimmed : null;
        };

        return new HtmlPolicyBuilder()
            .allowElements(
                "p", "br", "strong", "em", "b", "i", "u", "s", "blockquote", "code", "pre",
                "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6", "hr",
                "div", "span", "figure", "figcaption",
                "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                "a", "img", "video", "source", "iframe"
            )
            .allowAttributes("href").matching(linkHrefPolicy).onElements("a")
            .allowAttributes("title", "target", "rel").onElements("a")
            .allowAttributes("src").matching(mediaSrcPolicy).onElements("img", "video", "source")
            .allowAttributes("src").matching(iframeSrcPolicy).onElements("iframe")
            .allowAttributes("alt", "title", "width", "height").onElements("img")
            .allowAttributes("controls", "width", "height").onElements("video")
            .allowAttributes("poster").matching(mediaSrcPolicy).onElements("video")
            .allowAttributes("type").onElements("source")
            .allowAttributes("width", "height", "allow", "allowfullscreen", "frameborder", "title")
            .onElements("iframe")
            .allowAttributes("class", "data-id", "data-label", "data-type").onElements("span")
            .allowAttributes("data-youtube-video").onElements("div")
            .allowAttributes("colspan", "rowspan").onElements("th", "td")
            .allowUrlProtocols("http", "https")
            .toFactory();
    }

    private boolean isAllowedMediaSrc(String value) {
        if (isRelativeUrl(value)) {
            return true;
        }
        String origin = extractOrigin(value);
        return origin != null && allowedFileOrigins.contains(origin);
    }

    private boolean isAllowedIframeSrc(String value) {
        if (!value.startsWith("https://")) {
            return false;
        }
        return allowedIframePrefixes.stream().anyMatch(value::startsWith);
    }

    private boolean isAllowedLinkHref(String value) {
        if (isRelativeUrl(value)) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isRelativeUrl(String value) {
        if (value.startsWith("//")) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.getScheme() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String extractOrigin(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            if (port == -1) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + port;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Set<String> normalizeOrigins(List<String> origins) {
        if (origins == null) {
            return Set.of();
        }
        return origins.stream()
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(this::stripTrailingSlash)
            .collect(Collectors.toSet());
    }

    private String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == '/') {
            end -= 1;
        }
        return value.substring(0, end);
    }
}
