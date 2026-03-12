package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardFileEntity;

public interface BoardFileRepository extends JpaRepository<BoardFileEntity, Long> {
    @EntityGraph(attributePaths = {"board"})
    List<BoardFileEntity> findAllByFileId(Long fileId);

    List<BoardFileEntity> findAllByBoardIdOrderByCreatedAtDesc(Long boardId);

    List<BoardFileEntity> findAllByBoardIdInOrderByCreatedAtDesc(Collection<Long> boardIds);
}
