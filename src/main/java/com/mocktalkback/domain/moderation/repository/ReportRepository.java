package com.mocktalkback.domain.moderation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.moderation.entity.ReportEntity;
import com.mocktalkback.domain.moderation.type.ReportStatus;
import com.mocktalkback.domain.moderation.type.ReportTargetType;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    Page<ReportEntity> findAllByStatus(ReportStatus status, Pageable pageable);

    Page<ReportEntity> findAllByBoardId(Long boardId, Pageable pageable);

    Page<ReportEntity> findAllByBoardIdAndStatus(Long boardId, ReportStatus status, Pageable pageable);

    boolean existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(
        Long reporterUserId,
        ReportTargetType targetType,
        Long targetId,
        Iterable<ReportStatus> statuses
    );

    Optional<ReportEntity> findTopByReporterUserIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
        Long reporterUserId,
        ReportTargetType targetType,
        Long targetId
    );

    List<ReportEntity> findAllByBoardIdAndTargetTypeAndTargetIdIn(
        Long boardId,
        ReportTargetType targetType,
        Collection<Long> targetIds
    );
}
