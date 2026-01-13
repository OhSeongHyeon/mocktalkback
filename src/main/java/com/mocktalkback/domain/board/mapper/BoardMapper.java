package com.mocktalkback.domain.board.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardFileCreateRequest;
import com.mocktalkback.domain.board.dto.BoardFileResponse;
import com.mocktalkback.domain.board.dto.BoardMemberCreateRequest;
import com.mocktalkback.domain.board.dto.BoardMemberResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeCreateRequest;
import com.mocktalkback.domain.board.dto.BoardSubscribeResponse;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardFileEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface BoardMapper {

    BoardResponse toResponse(BoardEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "boardId", source = "board.id")
    @Mapping(target = "grantedByUserId", source = "grantedByUser.id")
    BoardMemberResponse toResponse(BoardMemberEntity entity);

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "boardId", source = "board.id")
    BoardFileResponse toResponse(BoardFileEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "boardId", source = "board.id")
    BoardSubscribeResponse toResponse(BoardSubscribeEntity entity);

    default BoardEntity toEntity(BoardCreateRequest request) {
        return BoardEntity.builder()
            .boardName(request.boardName())
            .slug(request.slug())
            .description(request.description())
            .visibility(request.visibility())
            .build();
    }

    default BoardMemberEntity toEntity(
        BoardMemberCreateRequest request,
        UserEntity user,
        BoardEntity board,
        UserEntity grantedByUser
    ) {
        return BoardMemberEntity.builder()
            .user(user)
            .board(board)
            .grantedByUser(grantedByUser)
            .boardRole(request.boardRole())
            .build();
    }

    default BoardFileEntity toEntity(
        BoardFileCreateRequest request,
        FileEntity file,
        BoardEntity board
    ) {
        return BoardFileEntity.builder()
            .file(file)
            .board(board)
            .build();
    }

    default BoardSubscribeEntity toEntity(
        BoardSubscribeCreateRequest request,
        UserEntity user,
        BoardEntity board
    ) {
        return BoardSubscribeEntity.builder()
            .user(user)
            .board(board)
            .build();
    }
}
