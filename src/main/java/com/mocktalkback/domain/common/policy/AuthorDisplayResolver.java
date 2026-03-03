package com.mocktalkback.domain.common.policy;

import org.springframework.stereotype.Component;

import com.mocktalkback.domain.user.entity.UserEntity;

@Component
public class AuthorDisplayResolver {

    public String resolveAuthorName(UserEntity user) {
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return user.getUserName();
    }

    public String formatOwnerDisplay(UserEntity user) {
        if (user == null) {
            return null;
        }
        String displayName = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        String handle = user.getHandle() == null ? "" : user.getHandle().trim();
        if (displayName.isEmpty() && handle.isEmpty()) {
            return null;
        }
        if (handle.isEmpty()) {
            return displayName;
        }
        if (displayName.isEmpty()) {
            return "@" + handle;
        }
        return displayName + "@" + handle;
    }
}

