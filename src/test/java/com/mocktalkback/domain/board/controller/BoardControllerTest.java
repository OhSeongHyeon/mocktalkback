package com.mocktalkback.domain.board.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardDetailResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.service.BoardService;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.global.common.dto.PageResponse;

@WebMvcTest(controllers = BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "DEV_SERVER_PORT=0"
})
class BoardControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private ArticleService articleService;

    // 게시판 생성 API는 성공 응답을 반환해야 한다.
    @Test
    void create_returns_ok() throws Exception {
        // Given: 게시판 생성 요청
        BoardCreateRequest request = new BoardCreateRequest(
            "notice",
            "notice",
            "notice board",
            BoardVisibility.PUBLIC
        );
        BoardResponse response = new BoardResponse(
            1L,
            "notice",
            "notice",
            "notice board",
            BoardVisibility.PUBLIC,
            FIXED_TIME,
            FIXED_TIME,
            null,
            null
        );
        when(boardService.create(any(BoardCreateRequest.class))).thenReturn(response);

        // When: 게시판 생성 API 호출
        ResultActions result = mockMvc.perform(post("/api/boards")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1L));
    }

    // 게시판 단건 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findById_returns_board() throws Exception {
        // Given: 게시판 응답
        BoardDetailResponse response = new BoardDetailResponse(
            1L,
            "notice",
            "notice",
            "notice board",
            BoardVisibility.PUBLIC,
            FIXED_TIME,
            FIXED_TIME,
            null,
            null,
            null,
            null,
            false
        );
        when(boardService.findById(1L)).thenReturn(response);

        // When: 게시판 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/boards/1"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.slug").value("notice"));
    }

    // 게시판 목록 조회 API는 리스트 응답을 반환해야 한다.
    @Test
    void findAll_returns_list() throws Exception {
        // Given: 게시판 목록 응답
        List<BoardResponse> responses = List.of(
            new BoardResponse(
                1L,
                "notice",
                "notice",
                "notice board",
                BoardVisibility.PUBLIC,
                FIXED_TIME,
                FIXED_TIME,
                null,
                null
            ),
            new BoardResponse(
                2L,
                "free",
                "free",
                "free board",
                BoardVisibility.PUBLIC,
                FIXED_TIME,
                FIXED_TIME,
                null,
                null
            )
        );
        PageResponse<BoardResponse> pageResponse = new PageResponse<>(
            responses,
            0,
            10,
            2,
            1,
            false,
            false
        );
        when(boardService.findAll(eq(0), eq(10))).thenReturn(pageResponse);

        // When: 게시판 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/boards")
            .param("page", "0")
            .param("size", "10"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].id").value(1L))
            .andExpect(jsonPath("$.data.items[1].id").value(2L))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10));
    }

    // 게시판 수정 API는 변경된 응답을 반환해야 한다.
    @Test
    void update_returns_updated_board() throws Exception {
        // Given: 게시판 수정 요청
        BoardUpdateRequest request = new BoardUpdateRequest(
            "notice updated",
            "notice",
            "notice updated",
            BoardVisibility.GROUP
        );
        BoardResponse response = new BoardResponse(
            1L,
            "notice updated",
            "notice",
            "notice updated",
            BoardVisibility.GROUP,
            FIXED_TIME,
            FIXED_TIME,
            null,
            null
        );
        when(boardService.update(1L, request)).thenReturn(response);

        // When: 게시판 수정 API 호출
        ResultActions result = mockMvc.perform(put("/api/boards/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.boardName").value("notice updated"));
    }

    // 게시판 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void delete_returns_ok() throws Exception {
        // Given: 게시판 삭제 준비
        doNothing().when(boardService).delete(1L);

        // When: 게시판 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/boards/1"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
