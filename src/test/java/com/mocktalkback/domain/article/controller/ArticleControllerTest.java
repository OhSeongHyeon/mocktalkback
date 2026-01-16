package com.mocktalkback.domain.article.controller;

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
import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleBoardResponse;
import com.mocktalkback.domain.article.dto.ArticleDetailResponse;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.role.type.ContentVisibility;

@WebMvcTest(controllers = ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "DEV_SERVER_PORT=0"
})
class ArticleControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticleService articleService;

    // 게시글 생성 API는 성공 응답을 반환해야 한다.
    @Test
    void create_returns_ok() throws Exception {
        // Given: 게시글 생성 요청
        ArticleCreateRequest request = new ArticleCreateRequest(
            1L,
            2L,
            3L,
            ContentVisibility.PUBLIC,
            "title",
            "content",
            false
        );
        ArticleResponse response = new ArticleResponse(
            10L,
            1L,
            2L,
            3L,
            ContentVisibility.PUBLIC,
            "title",
            "content",
            0L,
            false,
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(articleService.create(any(ArticleCreateRequest.class))).thenReturn(response);

        // When: 게시글 생성 API 호출
        ResultActions result = mockMvc.perform(post("/api/articles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(10L));
    }

    // 게시글 단건 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findById_returns_article() throws Exception {
        // Given: 게시글 응답
        ArticleBoardResponse boardResponse = new ArticleBoardResponse(
            1L,
            "notice",
            "notice",
            "notice board",
            BoardVisibility.PUBLIC,
            null
        );
        ArticleDetailResponse response = new ArticleDetailResponse(
            10L,
            boardResponse,
            2L,
            "author",
            ContentVisibility.PUBLIC,
            "title",
            "content",
            5L,
            0L,
            false,
            FIXED_TIME,
            FIXED_TIME,
            List.of()
        );
        when(articleService.findDetailById(10L, true)).thenReturn(response);

        // When: 게시글 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/articles/10"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(10L));
    }

    // 게시글 목록 조회 API는 리스트 응답을 반환해야 한다.
    @Test
    void findAll_returns_list() throws Exception {
        // Given: 게시글 목록 응답
        List<ArticleResponse> responses = List.of(
            new ArticleResponse(
                10L,
                1L,
                2L,
                3L,
                ContentVisibility.PUBLIC,
                "title",
                "content",
                0L,
                false,
                FIXED_TIME,
                FIXED_TIME,
                null
            ),
            new ArticleResponse(
                11L,
                1L,
                2L,
                null,
                ContentVisibility.PUBLIC,
                "title2",
                "content2",
                0L,
                false,
                FIXED_TIME,
                FIXED_TIME,
                null
            )
        );
        when(articleService.findAll()).thenReturn(responses);

        // When: 게시글 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/articles"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(10L))
            .andExpect(jsonPath("$.data[1].id").value(11L));
    }

    // 게시글 수정 API는 변경된 응답을 반환해야 한다.
    @Test
    void update_returns_updated_article() throws Exception {
        // Given: 게시글 수정 요청
        ArticleUpdateRequest request = new ArticleUpdateRequest(
            3L,
            ContentVisibility.MEMBERS,
            "updated title",
            "updated content",
            true
        );
        ArticleResponse response = new ArticleResponse(
            10L,
            1L,
            2L,
            3L,
            ContentVisibility.MEMBERS,
            "updated title",
            "updated content",
            0L,
            true,
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(articleService.update(10L, request)).thenReturn(response);

        // When: 게시글 수정 API 호출
        ResultActions result = mockMvc.perform(put("/api/articles/10")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("updated title"));
    }

    // 게시글 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void delete_returns_ok() throws Exception {
        // Given: 게시글 삭제 준비
        doNothing().when(articleService).delete(10L);

        // When: 게시글 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/articles/10"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
