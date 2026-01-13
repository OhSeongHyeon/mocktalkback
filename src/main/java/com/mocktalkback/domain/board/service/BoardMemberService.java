package com.mocktalkback.domain.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.board.dto.BoardMemberCreateRequest;
import com.mocktalkback.domain.board.dto.BoardMemberResponse;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardMemberRepository;
import com.mocktalkback.domain.board.repository.BoardRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardMemberResponse create(BoardMemberCreateRequest request) {
        UserEntity user = getUser(request.userId());
        BoardEntity board = getBoard(request.boardId());
        UserEntity grantedBy = getGrantedByUser(request.grantedByUserId());
        BoardMemberEntity entity = boardMapper.toEntity(request, user, board, grantedBy);
        BoardMemberEntity saved = boardMemberRepository.save(entity);
        return boardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BoardMemberResponse findById(Long id) {
        BoardMemberEntity entity = boardMemberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("board member not found: " + id));
        return boardMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BoardMemberResponse> findAll() {
        return boardMemberRepository.findAll().stream()
            .map(boardMapper::toResponse)
            .toList();
    }

    @Transactional
    public void delete(Long id) {
        boardMemberRepository.deleteById(id);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    private BoardEntity getBoard(Long boardId) {
        return boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + boardId));
    }

    private UserEntity getGrantedByUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("granted by user not found: " + userId));
    }
}
