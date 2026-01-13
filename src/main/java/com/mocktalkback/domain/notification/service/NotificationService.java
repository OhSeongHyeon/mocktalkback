package com.mocktalkback.domain.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.notification.dto.NotificationCreateRequest;
import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.dto.NotificationUpdateRequest;
import com.mocktalkback.domain.notification.entity.NotificationEntity;
import com.mocktalkback.domain.notification.mapper.NotificationMapper;
import com.mocktalkback.domain.notification.repository.NotificationRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request) {
        UserEntity user = getUser(request.userId());
        UserEntity sender = getSender(request.senderId());
        NotificationEntity entity = notificationMapper.toEntity(request, user, sender);
        NotificationEntity saved = notificationRepository.save(entity);
        return notificationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        NotificationEntity entity = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("notification not found: " + id));
        return notificationMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll() {
        return notificationRepository.findAll().stream()
            .map(notificationMapper::toResponse)
            .toList();
    }

    @Transactional
    public NotificationResponse update(Long id, NotificationUpdateRequest request) {
        NotificationEntity entity = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("notification not found: " + id));
        entity.updateRead(request.read());
        return notificationMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private UserEntity getSender(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("sender not found: " + userId));
    }
}
