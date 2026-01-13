package com.mocktalkback.dev;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.role.type.AuthBits;
import com.mocktalkback.domain.role.type.RoleNames;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

@Profile("dev")
@Configuration
public class Seed {

    private static final String SEED_PASSWORD = "123123123";

    @Bean
    ApplicationRunner seeder(
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> seed(roleRepository, userRepository, passwordEncoder);
    }

    @Transactional
    void seed(
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        upsertRole(roleRepository, RoleNames.USER, AuthBits.READ, "기본 사용자(읽기)");
        upsertRole(roleRepository, RoleNames.WRITER, AuthBits.READ | AuthBits.WRITE, "작성 가능(읽기+쓰기)");
        upsertRole(roleRepository, RoleNames.MANAGER, AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE, "모더레이터(읽기+쓰기+삭제)");
        upsertRole(roleRepository, RoleNames.ADMIN, AuthBits.READ | AuthBits.WRITE | AuthBits.DELETE | AuthBits.ADMIN, "전체 권한");
        seedUsers(roleRepository, userRepository, passwordEncoder);
    }

    private void upsertRole(RoleRepository repo, String roleName, int authBit, String description) {
        // 이미 있으면 스킵(또는 업데이트)
        if (repo.existsByRoleName(roleName)) return;
        repo.save(RoleEntity.create(roleName, authBit, description));
    }

    private void seedUsers(
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        seedUser(
            userRepository,
            roleRepository,
            RoleNames.USER,
            "seed_user",
            "seed_user@example.com",
            "Seed User",
            "SeedUser",
            "seed_user",
            passwordEncoder.encode(SEED_PASSWORD)
        );
        seedUser(
            userRepository,
            roleRepository,
            RoleNames.WRITER,
            "seed_writer",
            "seed_writer@example.com",
            "Seed Writer",
            "SeedWriter",
            "seed_writer",
            passwordEncoder.encode(SEED_PASSWORD)
        );
        seedUser(
            userRepository,
            roleRepository,
            RoleNames.MANAGER,
            "seed_moderator",
            "seed_moderator@example.com",
            "Seed Moderator",
            "SeedMod",
            "seed_moderator",
            passwordEncoder.encode(SEED_PASSWORD)
        );
        seedUser(
            userRepository,
            roleRepository,
            RoleNames.ADMIN,
            "seed_admin",
            "seed_admin@example.com",
            "Seed Admin",
            "SeedAdmin",
            "seed_admin",
            passwordEncoder.encode(SEED_PASSWORD)
        );
    }

    private void seedUser(
        UserRepository userRepository,
        RoleRepository roleRepository,
        String roleName,
        String loginId,
        String email,
        String userName,
        String displayName,
        String handle,
        String encodedPw
    ) {
        if (userRepository.existsByLoginId(loginId)
            || userRepository.existsByEmail(email)
            || userRepository.existsByHandle(handle)) {
            return;
        }
        RoleEntity role = roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new IllegalStateException("권한이 없습니다: " + roleName));
        UserEntity user = UserEntity.createLocal(
            role,
            loginId,
            email,
            encodedPw,
            userName,
            displayName,
            handle
        );
        userRepository.save(user);
    }
}
