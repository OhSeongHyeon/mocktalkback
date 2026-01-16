package com.mocktalkback.global.common.sanitize;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sanitize")
public class HtmlSanitizerProperties {

    private List<String> allowedFileOrigins = new ArrayList<>();
    private List<String> allowedIframePrefixes = new ArrayList<>();

    public List<String> getAllowedFileOrigins() {
        return allowedFileOrigins;
    }

    public void setAllowedFileOrigins(List<String> allowedFileOrigins) {
        this.allowedFileOrigins = allowedFileOrigins;
    }

    public List<String> getAllowedIframePrefixes() {
        return allowedIframePrefixes;
    }

    public void setAllowedIframePrefixes(List<String> allowedIframePrefixes) {
        this.allowedIframePrefixes = allowedIframePrefixes;
    }
}
