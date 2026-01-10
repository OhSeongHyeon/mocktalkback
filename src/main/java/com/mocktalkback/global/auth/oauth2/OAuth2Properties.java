package com.mocktalkback.global.auth.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    private long codeTtlSeconds = 60;
    private String redirectPath = "/oauth/callback";
    private boolean rememberMeDefault = false;

    public long getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(long codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void setRedirectPath(String redirectPath) {
        if (StringUtils.hasText(redirectPath)) {
            this.redirectPath = redirectPath.trim();
        }
    }

    public boolean isRememberMeDefault() {
        return rememberMeDefault;
    }

    public void setRememberMeDefault(boolean rememberMeDefault) {
        this.rememberMeDefault = rememberMeDefault;
    }
}
