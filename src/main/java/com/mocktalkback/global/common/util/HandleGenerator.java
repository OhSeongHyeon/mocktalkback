package com.mocktalkback.global.common.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HandleGenerator {

    private static final int HANDLE_LENGTH = 12;
    private static final int HANDLE_TRIES = 10;
    private static final char[] HANDLE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom HANDLE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    /**
     * 레이스 컨디션 주의: 동시에 같은 후보가 "없음"으로 판단될 수 있음.
     * 해결법
     * 1) DB 유니크 제약으로 최종 충돌 방어,
     * 2) 저장 시 DataIntegrityViolationException 발생하면 새 핸들로 재시도,
     * 3) 재시도는 rollback-only 회피를 위해 REQUIRES_NEW로 분리하는 방식이 안정적임.
     * 트랜잭션은 조회/저장 순서를 묶을 뿐 원자성을 보장하지 않으므로, 유니크 제약이 최종 방어선이다.
     */
    public String generateUniqueHandle() {
        for (int i = 0; i < HANDLE_TRIES; i++) {
            String candidate = randomHandle(HANDLE_LENGTH);
            boolean isHandleTaken = userRepository.existsByHandle(candidate);
            if (isHandleTaken) {
                continue;
            }
            return candidate;
        }
        throw new IllegalStateException("사용 가능한 핸들을 생성하지 못했습니다.");
    }

    private String randomHandle(int length) {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = HANDLE_CHARS[HANDLE_RANDOM.nextInt(HANDLE_CHARS.length)];
        }
        return new String(buffer);
    }
}
