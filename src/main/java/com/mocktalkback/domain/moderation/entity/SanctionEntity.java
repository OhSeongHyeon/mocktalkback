package com.mocktalkback.domain.moderation.entity;

import java.time.Instant;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.moderation.type.SanctionScopeType;
import com.mocktalkback.domain.moderation.type.SanctionType;
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
    name = "tb_sanctions",
    indexes = {
        @Index(
            name = "ix_tb_sanctions_user_id_ends_at",
            columnList = "user_id, ends_at"
        ),
        @Index(
            name = "ix_tb_sanctions_scope_type_board_id",
            columnList = "scope_type, board_id"
        ),
        @Index(
            name = "ix_tb_sanctions_report_id",
            columnList = "report_id"
        )
    }
)
public class SanctionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_sanctions_user_id__tb_users")
    )
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private SanctionScopeType scopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "board_id",
        foreignKey = @ForeignKey(name = "fk_tb_sanctions_board_id__tb_boards")
    )
    private BoardEntity board;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 24)
    private SanctionType sanctionType;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "report_id",
        foreignKey = @ForeignKey(name = "fk_tb_sanctions_report_id__tb_reports")
    )
    private ReportEntity report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_sanctions_created_by__tb_users")
    )
    private UserEntity createdBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "revoked_by",
        foreignKey = @ForeignKey(name = "fk_tb_sanctions_revoked_by__tb_users")
    )
    private UserEntity revokedBy;

    @Column(name = "revoked_reason", columnDefinition = "text")
    private String revokedReason;

    @Builder
    private SanctionEntity(
        UserEntity user,
        SanctionScopeType scopeType,
        BoardEntity board,
        SanctionType sanctionType,
        String reason,
        Instant startsAt,
        Instant endsAt,
        ReportEntity report,
        UserEntity createdBy
    ) {
        this.user = user;
        this.scopeType = scopeType;
        this.board = board;
        this.sanctionType = sanctionType;
        this.reason = reason;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.report = report;
        this.createdBy = createdBy;
    }

    public void revoke(UserEntity revokedBy, String revokedReason) {
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
        this.revokedReason = revokedReason;
    }
}
