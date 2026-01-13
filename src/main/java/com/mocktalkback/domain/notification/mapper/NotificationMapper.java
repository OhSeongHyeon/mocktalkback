package com.mocktalkback.domain.notification.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mocktalkback.domain.notification.dto.NotificationCreateRequest;
import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.entity.NotificationEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface NotificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "senderId", source = "sender.id")
    NotificationResponse toResponse(NotificationEntity entity);

    default NotificationEntity toEntity(
        NotificationCreateRequest request,
        UserEntity user,
        UserEntity sender
    ) {
        return NotificationEntity.builder()
            .user(user)
            .sender(sender)
            .notiType(request.notiType())
            .redirectUrl(request.redirectUrl())
            .referenceType(request.referenceType())
            .referenceId(request.referenceId())
            .read(request.read())
            .build();
    }
}
