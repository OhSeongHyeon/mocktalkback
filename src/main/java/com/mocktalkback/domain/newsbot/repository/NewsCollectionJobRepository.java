package com.mocktalkback.domain.newsbot.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;

public interface NewsCollectionJobRepository extends JpaRepository<NewsCollectionJobEntity, Long> {
    boolean existsByJobName(String jobName);

    Optional<NewsCollectionJobEntity> findByIdAndCreatedByUser_Id(Long id, Long createdByUserId);

    List<NewsCollectionJobEntity> findAllByOrderByCreatedAtDesc();

    List<NewsCollectionJobEntity> findTop20ByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant now);
}
