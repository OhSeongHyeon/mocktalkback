package com.mocktalkback.domain.moderation.entity;

import java.time.Instant;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.moderation.type.ReportReasonCode;
import com.mocktalkback.domain.moderation.type.ReportStatus;
import com.mocktalkback.domain.moderation.type.ReportTargetType;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_reports",
    indexes = {
        @Index(
            name = "ix_tb_reports_status_created_at",
            columnList = "status, created_at"
        ),
        @Index(
            name = "ix_tb_reports_board_id_status_created_at",
            columnList = "board_id, status, created_at"
        ),
        @Index(
            name = "ix_tb_reports_target_type_target_id",
            columnList = "target_type, target_id"
        ),
        @Index(
            name = "ix_tb_reports_reporter_user_id_created_at",
            columnList = "reporter_user_id, created_at"
        )
    }
)
public class ReportEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "reporter_user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_reports_reporter_user_id__tb_users")
    )
    private UserEntity reporterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "target_user_id",
        foreignKey = @ForeignKey(name = "fk_tb_reports_target_user_id__tb_users")
    )
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "board_id",
        foreignKey = @ForeignKey(name = "fk_tb_reports_board_id__tb_boards")
    )
    private BoardEntity board;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 24)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_snapshot", columnDefinition = "text")
    private String targetSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 24)
    private ReportReasonCode reasonCode;

    @Column(name = "reason_detail", columnDefinition = "text")
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processed_by",
        foreignKey = @ForeignKey(name = "fk_tb_reports_processed_by__tb_users")
    )
    private UserEntity processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed_note", columnDefinition = "text")
    private String processedNote;

    @Builder
    private ReportEntity(
        UserEntity reporterUser,
        UserEntity targetUser,
        BoardEntity board,
        ReportTargetType targetType,
        Long targetId,
        String targetSnapshot,
        ReportReasonCode reasonCode,
        String reasonDetail,
        ReportStatus status
    ) {
        this.reporterUser = reporterUser;
        this.targetUser = targetUser;
        this.board = board;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetSnapshot = targetSnapshot;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.status = status;
    }

    public void process(ReportStatus status, UserEntity processedBy, String processedNote) {
        this.status = status;
        this.processedBy = processedBy;
        this.processedNote = processedNote;
        this.processedAt = Instant.now();
    }
}
