package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    Page<BoardEntity> findAllByDeletedAtIsNull(Pageable pageable);

    List<BoardEntity> findAllByDeletedAtIsNull(Sort sort);

    Optional<BoardEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<BoardEntity> findBySlugAndDeletedAtIsNull(String slug);

    Page<BoardEntity> findAllByVisibilityInAndDeletedAtIsNull(
        Collection<BoardVisibility> visibilities,
        Pageable pageable
    );

    List<BoardEntity> findAllByVisibilityInAndDeletedAtIsNull(
        Collection<BoardVisibility> visibilities,
        Sort sort
    );
}
