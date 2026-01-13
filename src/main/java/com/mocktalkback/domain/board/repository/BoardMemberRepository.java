package com.mocktalkback.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.board.entity.BoardMemberEntity;

public interface BoardMemberRepository extends JpaRepository<BoardMemberEntity, Long> {
}
