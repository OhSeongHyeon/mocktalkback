package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardRole;
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

    @Query("""
        select b
        from BoardEntity b
        where (
            :includeDeleted = true
            or b.deletedAt is null
        )
          and (
            :keyword is null
            or lower(b.boardName) like concat('%', cast(:keyword as string), '%')
            or lower(b.slug) like concat('%', cast(:keyword as string), '%')
          )
          and (
            :visibility is null
            or b.visibility = :visibility
          )
        """)
    Page<BoardEntity> findAdminBoards(
        @Param("keyword") String keyword,
        @Param("visibility") BoardVisibility visibility,
        @Param("includeDeleted") boolean includeDeleted,
        Pageable pageable
    );

    @Query(
        value = """
            select distinct b
            from BoardEntity b
            left join BoardMemberEntity bm
                on bm.board.id = b.id and bm.user.id = :userId
            where b.deletedAt is null
              and (bm.id is null or bm.boardRole <> :bannedRole)
              and (
                b.visibility in :visibleVisibilities
                or (b.visibility = :privateVisibility and bm.boardRole = :ownerRole)
              )
            """,
        countQuery = """
            select count(distinct b.id)
            from BoardEntity b
            left join BoardMemberEntity bm
                on bm.board.id = b.id and bm.user.id = :userId
            where b.deletedAt is null
              and (bm.id is null or bm.boardRole <> :bannedRole)
              and (
                b.visibility in :visibleVisibilities
                or (b.visibility = :privateVisibility and bm.boardRole = :ownerRole)
              )
            """
    )
    Page<BoardEntity> findAccessibleBoards(
        @Param("userId") Long userId,
        @Param("visibleVisibilities") Collection<BoardVisibility> visibleVisibilities,
        @Param("privateVisibility") BoardVisibility privateVisibility,
        @Param("ownerRole") BoardRole ownerRole,
        @Param("bannedRole") BoardRole bannedRole,
        Pageable pageable
    );
}
