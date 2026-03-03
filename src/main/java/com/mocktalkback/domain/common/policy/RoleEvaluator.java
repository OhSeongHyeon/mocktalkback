package com.mocktalkback.domain.common.policy;

import org.springframework.stereotype.Component;

import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;

@Component
public class RoleEvaluator {

    public boolean isAdmin(UserEntity user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return RoleNames.ADMIN.equals(user.getRole().getRoleName());
    }

    public boolean isManagerOrAdmin(UserEntity user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String roleName = user.getRole().getRoleName();
        return RoleNames.MANAGER.equals(roleName) || RoleNames.ADMIN.equals(roleName);
    }
}
