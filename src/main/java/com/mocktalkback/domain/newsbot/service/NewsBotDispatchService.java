package com.mocktalkback.domain.newsbot.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;
import com.mocktalkback.domain.newsbot.repository.NewsCollectionJobRepository;

@Service
public class NewsBotDispatchService {

    private final NewsCollectionJobRepository newsCollectionJobRepository;
    private final NewsBotJobExecutor newsBotJobExecutor;
    private final Clock clock;

    @Autowired
    public NewsBotDispatchService(
        NewsCollectionJobRepository newsCollectionJobRepository,
        NewsBotJobExecutor newsBotJobExecutor
    ) {
        this(newsCollectionJobRepository, newsBotJobExecutor, Clock.systemUTC());
    }

    NewsBotDispatchService(
        NewsCollectionJobRepository newsCollectionJobRepository,
        NewsBotJobExecutor newsBotJobExecutor,
        Clock clock
    ) {
        this.newsCollectionJobRepository = newsCollectionJobRepository;
        this.newsBotJobExecutor = newsBotJobExecutor;
        this.clock = clock;
    }

    public void dispatchDueJobs() {
        Instant now = clock.instant();
        List<NewsCollectionJobEntity> dueJobs = newsCollectionJobRepository
            .findTop20ByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(now);

        for (NewsCollectionJobEntity dueJob : dueJobs) {
            newsBotJobExecutor.runScheduled(dueJob.getId());
        }
    }
}
