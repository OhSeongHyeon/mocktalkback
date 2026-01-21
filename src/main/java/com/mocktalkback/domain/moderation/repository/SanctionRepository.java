package com.mocktalkback.domain.moderation.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.moderation.entity.SanctionEntity;
import com.mocktalkback.domain.moderation.type.SanctionScopeType;

public interface SanctionRepository extends JpaRepository<SanctionEntity, Long> {
    Page<SanctionEntity> findAllByScopeType(SanctionScopeType scopeType, Pageable pageable);

    Page<SanctionEntity> findAllByScopeTypeAndBoardId(SanctionScopeType scopeType, Long boardId, Pageable pageable);

    Page<SanctionEntity> findAllByBoardId(Long boardId, Pageable pageable);

    @Query("""
        select case when count(s.id) > 0 then true else false end
        from SanctionEntity s
        where s.user.id = :userId
          and s.revokedAt is null
          and s.startsAt <= :now
          and (s.endsAt is null or s.endsAt > :now)
          and (
            s.scopeType = :globalScope
            or (s.scopeType = :boardScope and s.board.id = :boardId)
          )
        """)
    boolean existsActiveSanction(
        @Param("userId") Long userId,
        @Param("globalScope") SanctionScopeType globalScope,
        @Param("boardScope") SanctionScopeType boardScope,
        @Param("boardId") Long boardId,
        @Param("now") Instant now
    );
}
