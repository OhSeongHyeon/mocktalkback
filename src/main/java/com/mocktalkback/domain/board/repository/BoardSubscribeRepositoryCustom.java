package com.mocktalkback.domain.board.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;

public interface BoardSubscribeRepositoryCustom {
    Page<BoardSubscribeEntity> findAccessibleSubscribes(
        Long userId,
        Collection<BoardVisibility> visibleVisibilities,
        BoardVisibility privateVisibility,
        BoardRole ownerRole,
        BoardRole bannedRole,
        Pageable pageable
    );
}
