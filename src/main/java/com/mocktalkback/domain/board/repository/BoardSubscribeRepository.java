package com.mocktalkback.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;

public interface BoardSubscribeRepository extends JpaRepository<BoardSubscribeEntity, Long> {
    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    void deleteByUserIdAndBoardId(Long userId, Long boardId);
}
