package com.mocktalkback.domain.newsbot.entity;

import java.time.Instant;

import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;
import com.mocktalkback.domain.newsbot.type.NewsSourceType;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_news_collection_jobs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_news_collection_jobs_job_name", columnNames = "job_name")
    },
    indexes = {
        @Index(name = "ix_tb_news_collection_jobs_enabled_next_run_at", columnList = "is_enabled, next_run_at"),
        @Index(name = "ix_tb_news_collection_jobs_source_type", columnList = "source_type"),
        @Index(name = "ix_tb_news_collection_jobs_author_user_id", columnList = "author_user_id"),
        @Index(name = "ix_tb_news_collection_jobs_created_by_user_id", columnList = "created_by_user_id")
    }
)
public class NewsCollectionJobEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_job_id", nullable = false)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 120)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private NewsSourceType sourceType;

    @Column(name = "source_config_json", nullable = false, columnDefinition = "text")
    private String sourceConfigJson;

    @Column(name = "target_board_slug", nullable = false, length = 80)
    private String targetBoardSlug;

    @Column(name = "target_board_name", length = 255)
    private String targetBoardName;

    @Column(name = "target_category_name", length = 48)
    private String targetCategoryName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "author_user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_news_collection_jobs_author_user_id__tb_users")
    )
    private UserEntity authorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_news_collection_jobs_created_by_user_id__tb_users")
    )
    private UserEntity createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "updated_by_user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_news_collection_jobs_updated_by_user_id__tb_users")
    )
    private UserEntity updatedByUser;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "collect_interval_minutes", nullable = false)
    private int collectIntervalMinutes;

    @Column(name = "fetch_limit", nullable = false)
    private int fetchLimit;

    @Column(name = "is_auto_create_board", nullable = false)
    private boolean autoCreateBoard;

    @Column(name = "is_auto_create_category", nullable = false)
    private boolean autoCreateCategory;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "last_started_at")
    private Instant lastStartedAt;

    @Column(name = "last_finished_at")
    private Instant lastFinishedAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", nullable = false, length = 24)
    private NewsJobExecutionStatus lastStatus;

    @Column(name = "last_error_message", columnDefinition = "text")
    private String lastErrorMessage;

    @Builder
    private NewsCollectionJobEntity(
        String jobName,
        NewsSourceType sourceType,
        String sourceConfigJson,
        String targetBoardSlug,
        String targetBoardName,
        String targetCategoryName,
        UserEntity authorUser,
        UserEntity createdByUser,
        UserEntity updatedByUser,
        boolean enabled,
        int collectIntervalMinutes,
        int fetchLimit,
        boolean autoCreateBoard,
        boolean autoCreateCategory,
        String timezone,
        Instant nextRunAt,
        NewsJobExecutionStatus lastStatus
    ) {
        this.jobName = jobName;
        this.sourceType = sourceType;
        this.sourceConfigJson = sourceConfigJson;
        this.targetBoardSlug = targetBoardSlug;
        this.targetBoardName = targetBoardName;
        this.targetCategoryName = targetCategoryName;
        this.authorUser = authorUser;
        this.createdByUser = createdByUser;
        this.updatedByUser = updatedByUser;
        this.enabled = enabled;
        this.collectIntervalMinutes = collectIntervalMinutes;
        this.fetchLimit = fetchLimit;
        this.autoCreateBoard = autoCreateBoard;
        this.autoCreateCategory = autoCreateCategory;
        this.timezone = timezone;
        this.nextRunAt = nextRunAt;
        this.lastStatus = lastStatus == null ? NewsJobExecutionStatus.IDLE : lastStatus;
    }

    public static NewsCollectionJobEntity create(
        String jobName,
        NewsSourceType sourceType,
        String sourceConfigJson,
        String targetBoardSlug,
        String targetBoardName,
        String targetCategoryName,
        UserEntity authorUser,
        UserEntity actor,
        int collectIntervalMinutes,
        int fetchLimit,
        boolean autoCreateBoard,
        boolean autoCreateCategory,
        String timezone,
        Instant nextRunAt
    ) {
        return NewsCollectionJobEntity.builder()
            .jobName(jobName)
            .sourceType(sourceType)
            .sourceConfigJson(sourceConfigJson)
            .targetBoardSlug(targetBoardSlug)
            .targetBoardName(targetBoardName)
            .targetCategoryName(targetCategoryName)
            .authorUser(authorUser)
            .createdByUser(actor)
            .updatedByUser(actor)
            .enabled(true)
            .collectIntervalMinutes(collectIntervalMinutes)
            .fetchLimit(fetchLimit)
            .autoCreateBoard(autoCreateBoard)
            .autoCreateCategory(autoCreateCategory)
            .timezone(timezone)
            .nextRunAt(nextRunAt)
            .lastStatus(NewsJobExecutionStatus.IDLE)
            .build();
    }

    public void updateJob(
        String jobName,
        NewsSourceType sourceType,
        String sourceConfigJson,
        String targetBoardSlug,
        String targetBoardName,
        String targetCategoryName,
        UserEntity authorUser,
        UserEntity actor,
        int collectIntervalMinutes,
        int fetchLimit,
        boolean autoCreateBoard,
        boolean autoCreateCategory,
        String timezone,
        Instant nextRunAt
    ) {
        this.jobName = jobName;
        this.sourceType = sourceType;
        this.sourceConfigJson = sourceConfigJson;
        this.targetBoardSlug = targetBoardSlug;
        this.targetBoardName = targetBoardName;
        this.targetCategoryName = targetCategoryName;
        this.authorUser = authorUser;
        this.updatedByUser = actor;
        this.collectIntervalMinutes = collectIntervalMinutes;
        this.fetchLimit = fetchLimit;
        this.autoCreateBoard = autoCreateBoard;
        this.autoCreateCategory = autoCreateCategory;
        this.timezone = timezone;
        this.nextRunAt = nextRunAt;
    }

    public void changeEnabled(boolean enabled, UserEntity actor) {
        this.enabled = enabled;
        this.updatedByUser = actor;
    }

    public void scheduleNextRun(Instant nextRunAt, UserEntity actor) {
        this.nextRunAt = nextRunAt;
        this.updatedByUser = actor;
    }

    public void markRunning(Instant startedAt) {
        this.lastStartedAt = startedAt;
        this.lastStatus = NewsJobExecutionStatus.RUNNING;
        this.lastErrorMessage = null;
    }

    public void markSuccess(Instant finishedAt, Instant nextRunAt) {
        this.lastFinishedAt = finishedAt;
        this.lastSuccessAt = finishedAt;
        this.nextRunAt = nextRunAt;
        this.lastStatus = NewsJobExecutionStatus.SUCCESS;
        this.lastErrorMessage = null;
    }

    public void markFailure(Instant finishedAt, Instant nextRunAt, String lastErrorMessage) {
        this.lastFinishedAt = finishedAt;
        this.nextRunAt = nextRunAt;
        this.lastStatus = NewsJobExecutionStatus.FAILED;
        this.lastErrorMessage = lastErrorMessage;
    }
}
