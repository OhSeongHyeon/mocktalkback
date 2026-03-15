package com.mocktalkback.domain.newsbot.entity;

import java.time.Instant;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.newsbot.type.NewsCollectedItemSyncStatus;
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
    name = "tb_news_collected_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_news_collected_items_news_job_id_external_item_key",
            columnNames = {"news_job_id", "external_item_key"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_news_collected_items_article_id", columnList = "article_id"),
        @Index(name = "ix_tb_news_collected_items_news_job_id_last_sync_status", columnList = "news_job_id, last_sync_status"),
        @Index(name = "ix_tb_news_collected_items_news_job_id_published_at", columnList = "news_job_id, published_at")
    }
)
public class NewsCollectedItemEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_collected_item_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "news_job_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_news_collected_items_news_job_id__tb_news_collection_jobs")
    )
    private NewsCollectionJobEntity newsJob;

    @Column(name = "external_item_key", nullable = false, length = 255)
    private String externalItemKey;

    @Column(name = "external_url", nullable = false, columnDefinition = "text")
    private String externalUrl;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "article_id",
        foreignKey = @ForeignKey(name = "fk_tb_news_collected_items_article_id__tb_articles")
    )
    private ArticleEntity article;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", nullable = false, length = 24)
    private NewsCollectedItemSyncStatus lastSyncStatus;

    @Column(name = "last_error_message", columnDefinition = "text")
    private String lastErrorMessage;

    @Column(name = "first_collected_at", nullable = false)
    private Instant firstCollectedAt;

    @Column(name = "last_collected_at", nullable = false)
    private Instant lastCollectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Builder
    private NewsCollectedItemEntity(
        NewsCollectionJobEntity newsJob,
        String externalItemKey,
        String externalUrl,
        String title,
        String payloadHash,
        Instant publishedAt,
        Instant sourceUpdatedAt,
        ArticleEntity article,
        NewsCollectedItemSyncStatus lastSyncStatus,
        String lastErrorMessage,
        Instant firstCollectedAt,
        Instant lastCollectedAt,
        Instant lastSyncedAt
    ) {
        this.newsJob = newsJob;
        this.externalItemKey = externalItemKey;
        this.externalUrl = externalUrl;
        this.title = title;
        this.payloadHash = payloadHash;
        this.publishedAt = publishedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.article = article;
        this.lastSyncStatus = lastSyncStatus;
        this.lastErrorMessage = lastErrorMessage;
        this.firstCollectedAt = firstCollectedAt;
        this.lastCollectedAt = lastCollectedAt;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static NewsCollectedItemEntity create(
        NewsCollectionJobEntity newsJob,
        String externalItemKey,
        String externalUrl,
        String title,
        String payloadHash,
        Instant publishedAt,
        Instant sourceUpdatedAt,
        Instant collectedAt
    ) {
        return NewsCollectedItemEntity.builder()
            .newsJob(newsJob)
            .externalItemKey(externalItemKey)
            .externalUrl(externalUrl)
            .title(title)
            .payloadHash(payloadHash)
            .publishedAt(publishedAt)
            .sourceUpdatedAt(sourceUpdatedAt)
            .lastSyncStatus(NewsCollectedItemSyncStatus.SKIPPED)
            .firstCollectedAt(collectedAt)
            .lastCollectedAt(collectedAt)
            .build();
    }

    public void markCreated(ArticleEntity article, String payloadHash, Instant collectedAt) {
        this.article = article;
        this.payloadHash = payloadHash;
        this.lastSyncStatus = NewsCollectedItemSyncStatus.CREATED;
        this.lastErrorMessage = null;
        this.lastCollectedAt = collectedAt;
        this.lastSyncedAt = collectedAt;
    }

    public void markUpdated(ArticleEntity article, String payloadHash, Instant collectedAt) {
        this.article = article;
        this.payloadHash = payloadHash;
        this.lastSyncStatus = NewsCollectedItemSyncStatus.UPDATED;
        this.lastErrorMessage = null;
        this.lastCollectedAt = collectedAt;
        this.lastSyncedAt = collectedAt;
    }

    public void markSkipped(String payloadHash, Instant collectedAt) {
        this.payloadHash = payloadHash;
        this.lastSyncStatus = NewsCollectedItemSyncStatus.SKIPPED;
        this.lastErrorMessage = null;
        this.lastCollectedAt = collectedAt;
    }

    public void markFailure(String payloadHash, Instant collectedAt, String lastErrorMessage) {
        this.payloadHash = payloadHash;
        this.lastSyncStatus = NewsCollectedItemSyncStatus.FAILED;
        this.lastErrorMessage = lastErrorMessage;
        this.lastCollectedAt = collectedAt;
    }

    public void refreshSourceSnapshot(
        String externalUrl,
        String title,
        Instant publishedAt,
        Instant sourceUpdatedAt
    ) {
        this.externalUrl = externalUrl;
        this.title = title;
        this.publishedAt = publishedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }
}
