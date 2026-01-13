package com.mocktalkback.domain.comment.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
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
import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentResponse;
import com.mocktalkback.domain.comment.dto.CommentUpdateRequest;
import com.mocktalkback.domain.comment.service.CommentService;

@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "DEV_SERVER_PORT=0"
})
class CommentControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    // 댓글 생성 API는 성공 응답을 반환해야 한다.
    @Test
    void create_returns_ok() throws Exception {
        // Given: 댓글 생성 요청
        CommentCreateRequest request = new CommentCreateRequest(
            1L,
            10L,
            null,
            null,
            0,
            "comment content"
        );
        CommentResponse response = new CommentResponse(
            100L,
            1L,
            10L,
            null,
            null,
            0,
            "comment content",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(commentService.create(any(CommentCreateRequest.class))).thenReturn(response);

        // When: 댓글 생성 API 호출
        ResultActions result = mockMvc.perform(post("/api/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(100L));
    }

    // 댓글 단건 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findById_returns_comment() throws Exception {
        // Given: 댓글 응답
        CommentResponse response = new CommentResponse(
            100L,
            1L,
            10L,
            null,
            null,
            0,
            "comment content",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(commentService.findById(100L)).thenReturn(response);

        // When: 댓글 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/comments/100"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value("comment content"));
    }

    // 댓글 목록 조회 API는 리스트 응답을 반환해야 한다.
    @Test
    void findAll_returns_list() throws Exception {
        // Given: 댓글 목록 응답
        List<CommentResponse> responses = List.of(
            new CommentResponse(
                100L,
                1L,
                10L,
                null,
                null,
                0,
                "comment 1",
                FIXED_TIME,
                FIXED_TIME,
                null
            ),
            new CommentResponse(
                101L,
                1L,
                10L,
                100L,
                100L,
                1,
                "reply",
                FIXED_TIME,
                FIXED_TIME,
                null
            )
        );
        when(commentService.findAll()).thenReturn(responses);

        // When: 댓글 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/comments"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(100L))
            .andExpect(jsonPath("$.data[1].id").value(101L));
    }

    // 댓글 수정 API는 변경된 응답을 반환해야 한다.
    @Test
    void update_returns_updated_comment() throws Exception {
        // Given: 댓글 수정 요청
        CommentUpdateRequest request = new CommentUpdateRequest("updated comment");
        CommentResponse response = new CommentResponse(
            100L,
            1L,
            10L,
            null,
            null,
            0,
            "updated comment",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(commentService.update(100L, request)).thenReturn(response);

        // When: 댓글 수정 API 호출
        ResultActions result = mockMvc.perform(put("/api/comments/100")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value("updated comment"));
    }

    // 댓글 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void delete_returns_ok() throws Exception {
        // Given: 댓글 삭제 준비
        doNothing().when(commentService).delete(100L);

        // When: 댓글 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/comments/100"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
