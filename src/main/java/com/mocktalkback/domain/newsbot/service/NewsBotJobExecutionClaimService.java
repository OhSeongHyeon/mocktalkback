package com.mocktalkback.domain.newsbot.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.newsbot.config.NewsBotProperties;
import com.mocktalkback.domain.newsbot.repository.NewsCollectionJobRepository;
import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsBotJobExecutionClaimService {

    private final NewsCollectionJobRepository newsCollectionJobRepository;
    private final NewsBotProperties newsBotProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claimManualRun(Long jobId, Instant startedAt) {
        int updatedCount = newsCollectionJobRepository.claimManualRun(
            jobId,
            startedAt,
            resolveStaleStartedBefore(startedAt),
            NewsJobExecutionStatus.RUNNING
        );
        if (updatedCount > 0) {
            return;
        }
        if (!newsCollectionJobRepository.existsById(jobId)) {
            throw new IllegalArgumentException("뉴스봇 잡을 찾을 수 없습니다: " + jobId);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 실행 중인 뉴스봇 잡입니다.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimScheduledRun(Long jobId, Instant startedAt) {
        int updatedCount = newsCollectionJobRepository.claimScheduledRun(
            jobId,
            startedAt,
            resolveStaleStartedBefore(startedAt),
            NewsJobExecutionStatus.RUNNING
        );
        return updatedCount > 0;
    }

    private Instant resolveStaleStartedBefore(Instant startedAt) {
        return startedAt.minus(newsBotProperties.getRunLockTimeoutMinutes(), ChronoUnit.MINUTES);
    }
}
