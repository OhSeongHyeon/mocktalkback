package com.mocktalkback.domain.newsbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.newsbot.entity.NewsCollectedItemEntity;

public interface NewsCollectedItemRepository extends JpaRepository<NewsCollectedItemEntity, Long> {
    Optional<NewsCollectedItemEntity> findByNewsJob_IdAndExternalItemKey(Long newsJobId, String externalItemKey);

    List<NewsCollectedItemEntity> findTop50ByNewsJob_IdOrderByPublishedAtDescIdDesc(Long newsJobId);
}
