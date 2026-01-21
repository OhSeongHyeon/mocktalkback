package com.mocktalkback.domain.moderation.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "tb_admin_audit_logs",
    indexes = {
        @Index(
            name = "ix_tb_admin_audit_logs_actor_user_id_created_at",
            columnList = "actor_user_id, created_at"
        ),
        @Index(
            name = "ix_tb_admin_audit_logs_action_type_created_at",
            columnList = "action_type, created_at"
        ),
        @Index(
            name = "ix_tb_admin_audit_logs_target_type_target_id",
            columnList = "target_type, target_id"
        )
    }
)
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_log_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "actor_user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_admin_audit_logs_actor_user_id__tb_users")
    )
    private UserEntity actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 48)
    private AdminActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 24)
    private AdminTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "board_id",
        foreignKey = @ForeignKey(name = "fk_tb_admin_audit_logs_board_id__tb_boards")
    )
    private BoardEntity board;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private AdminAuditLogEntity(
        UserEntity actorUser,
        AdminActionType actionType,
        AdminTargetType targetType,
        Long targetId,
        BoardEntity board,
        String summary,
        String detailJson,
        String ipAddress,
        String userAgent
    ) {
        this.actorUser = actorUser;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.board = board;
        this.summary = summary;
        this.detailJson = detailJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
