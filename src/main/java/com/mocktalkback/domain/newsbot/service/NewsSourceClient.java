package com.mocktalkback.domain.newsbot.service;

import java.util.List;
import java.util.Map;

import com.mocktalkback.domain.newsbot.type.NewsSourceType;

public interface NewsSourceClient {
    NewsSourceType supports();

    void validateConfig(Map<String, Object> sourceConfig);

    List<NewsBotSourceItem> fetchItems(Map<String, Object> sourceConfig, int limit);
}
