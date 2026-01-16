package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardFileEntity;

public interface BoardFileRepository extends JpaRepository<BoardFileEntity, Long> {
    List<BoardFileEntity> findAllByBoardIdOrderByCreatedAtDesc(Long boardId);

    List<BoardFileEntity> findAllByBoardIdInOrderByCreatedAtDesc(Collection<Long> boardIds);
}
