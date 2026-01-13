package com.mocktalkback.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardEntity;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
}
