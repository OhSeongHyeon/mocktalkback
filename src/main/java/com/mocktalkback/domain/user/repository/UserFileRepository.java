package com.mocktalkback.domain.user.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.user.entity.UserFileEntity;

public interface UserFileRepository extends JpaRepository<UserFileEntity, Long> {

    @Query("""
        select uf from UserFileEntity uf
        join uf.file f
        join f.fileClass fc
        where uf.user.id = :userId
          and fc.code = :fileClassCode
        """)
    List<UserFileEntity> findAllByUserIdAndFileClassCode(
        @Param("userId") Long userId,
        @Param("fileClassCode") String fileClassCode
    );

    @Query("""
        select uf from UserFileEntity uf
        join uf.file f
        join f.fileClass fc
        where uf.user.id = :userId
          and fc.code = :fileClassCode
        order by uf.createdAt desc
        """)
    List<UserFileEntity> findLatestByUserIdAndFileClassCode(
        @Param("userId") Long userId,
        @Param("fileClassCode") String fileClassCode,
        Pageable pageable
    );
}
