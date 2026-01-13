package com.mocktalkback.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.notification.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
}
