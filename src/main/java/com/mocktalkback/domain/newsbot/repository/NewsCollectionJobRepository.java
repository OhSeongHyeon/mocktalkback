package com.mocktalkback.domain.newsbot.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;
import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;

public interface NewsCollectionJobRepository extends JpaRepository<NewsCollectionJobEntity, Long> {
    boolean existsByJobName(String jobName);

    Optional<NewsCollectionJobEntity> findByIdAndCreatedByUser_Id(Long id, Long createdByUserId);

    List<NewsCollectionJobEntity> findAllByOrderByCreatedAtDesc();

    List<NewsCollectionJobEntity> findTop20ByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update NewsCollectionJobEntity job
           set job.lastStartedAt = :startedAt,
               job.lastStatus = :runningStatus,
               job.lastErrorMessage = null
         where job.id = :jobId
           and (
               job.lastStatus <> :runningStatus
               or job.lastStartedAt is null
               or job.lastStartedAt < :staleStartedBefore
           )
        """)
    int claimManualRun(
        @Param("jobId") Long jobId,
        @Param("startedAt") Instant startedAt,
        @Param("staleStartedBefore") Instant staleStartedBefore,
        @Param("runningStatus") NewsJobExecutionStatus runningStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update NewsCollectionJobEntity job
           set job.lastStartedAt = :startedAt,
               job.lastStatus = :runningStatus,
               job.lastErrorMessage = null
         where job.id = :jobId
           and job.enabled = true
           and job.nextRunAt is not null
           and job.nextRunAt <= :startedAt
           and (
               job.lastStatus <> :runningStatus
               or job.lastStartedAt is null
               or job.lastStartedAt < :staleStartedBefore
           )
        """)
    int claimScheduledRun(
        @Param("jobId") Long jobId,
        @Param("startedAt") Instant startedAt,
        @Param("staleStartedBefore") Instant staleStartedBefore,
        @Param("runningStatus") NewsJobExecutionStatus runningStatus
    );
}
