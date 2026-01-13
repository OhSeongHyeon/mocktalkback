package com.mocktalkback.domain.notification.entity;

import com.mocktalkback.domain.common.entity.BaseTimeEntity;
import com.mocktalkback.domain.notification.type.NotificationType;
import com.mocktalkback.domain.notification.type.ReferenceType;
import com.mocktalkback.domain.user.entity.UserEntity;

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
    name = "tb_notification",
    indexes = {
        @Index(
            name = "ix_tb_notification_user_id_is_read_created_at",
            columnList = "user_id, is_read, created_at"
        )
    }
)
public class NotificationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_notification_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "sender_id",
        foreignKey = @ForeignKey(name = "fk_tb_notification_sender_id__tb_users")
    )
    private UserEntity sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type", nullable = false, length = 20)
    private NotificationType notiType;

    @Column(name = "redirect_url", length = 1024)
    private String redirectUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 24)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Builder
    private NotificationEntity(
        UserEntity user,
        UserEntity sender,
        NotificationType notiType,
        String redirectUrl,
        ReferenceType referenceType,
        Long referenceId,
        boolean read
    ) {
        this.user = user;
        this.sender = sender;
        this.notiType = notiType;
        this.redirectUrl = redirectUrl;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.read = read;
    }
}
