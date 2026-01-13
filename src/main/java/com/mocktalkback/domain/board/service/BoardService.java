package com.mocktalkback.domain.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.mapper.BoardMapper;
import com.mocktalkback.domain.board.repository.BoardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardResponse create(BoardCreateRequest request) {
        BoardEntity entity = boardMapper.toEntity(request);
        BoardEntity saved = boardRepository.save(entity);
        return boardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BoardResponse findById(Long id) {
        BoardEntity entity = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + id));
        return boardMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> findAll() {
        return boardRepository.findAll().stream()
            .map(boardMapper::toResponse)
            .toList();
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        BoardEntity entity = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("board not found: " + id));
        entity.update(request.boardName(), request.slug(), request.description(), request.visibility());
        return boardMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        boardRepository.deleteById(id);
    }
}
