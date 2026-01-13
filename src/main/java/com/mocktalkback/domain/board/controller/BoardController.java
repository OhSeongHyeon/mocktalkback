package com.mocktalkback.domain.board.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.service.BoardService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    public ApiEnvelope<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        return ApiEnvelope.ok(boardService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<BoardResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.findById(id));
    }

    @GetMapping
    public ApiEnvelope<List<BoardResponse>> findAll() {
        return ApiEnvelope.ok(boardService.findAll());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<BoardResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid BoardUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        boardService.delete(id);
        return ApiEnvelope.ok();
    }
}
