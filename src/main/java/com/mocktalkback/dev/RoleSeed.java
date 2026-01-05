package com.mocktalkback.dev;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.role.type.AuthBits;
import com.mocktalkback.domain.role.type.RoleNames;

@Profile("dev")
@Configuration
public class RoleSeed {

    @Bean
    ApplicationRunner roleSeeder(RoleRepository roleRepository) {
        return args -> seed(roleRepository);
    }

    @Transactional
    void seed(RoleRepository roleRepository) {
        upsertRole(roleRepository, RoleNames.USER, AuthBits.READ, "기본 사용자(읽기)");
        upsertRole(roleRepository, RoleNames.WRITER, AuthBits.READ | AuthBits.WRITE, "작성 가능(읽기+쓰기)");
        upsertRole(roleRepository, RoleNames.MODERATOR, AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE, "모더레이터(읽기+쓰기+삭제)");
        upsertRole(roleRepository, RoleNames.ADMIN, AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE | AuthBits.ADMIN, "전체 권한");
    }

    private void upsertRole(RoleRepository repo, String roleName, int authBit, String description) {
        // 이미 있으면 스킵(또는 업데이트)
        if (repo.existsByRoleName(roleName)) return;

        repo.save(RoleEntity.create(roleName, authBit, description));
    }
}
