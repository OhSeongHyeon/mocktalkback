package com.mocktalkback.domain.board.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocktalkback.domain.article.dto.ArticleSummaryResponse;
import com.mocktalkback.domain.article.dto.BoardArticleListResponse;
import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardDetailResponse;
import com.mocktalkback.domain.board.dto.BoardMemberStatusResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeItemResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeStatusResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.service.BoardService;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.global.common.dto.PageResponse;
import com.mocktalkback.global.common.type.SortOrder;

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

    // 게시판 슬러그 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findBySlug_returns_board() throws Exception {
        // Given: 게시판 응답
        BoardDetailResponse response = new BoardDetailResponse(
            2L,
            "free",
            "free",
            "free board",
            BoardVisibility.PUBLIC,
            FIXED_TIME,
            FIXED_TIME,
            null,
            null,
            null,
            null,
            false
        );
        when(boardService.findBySlug("free")).thenReturn(response);

        // When: 게시판 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/boards/slug/free"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.slug").value("free"));
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

    // 게시판 구독 목록 API는 리스트 응답을 반환해야 한다.
    @Test
    void findSubscribes_returns_list() throws Exception {
        // Given: 구독 목록 응답
        List<BoardSubscribeItemResponse> responses = List.of(
            new BoardSubscribeItemResponse(
                10L,
                1L,
                "notice",
                "notice",
                "notice board",
                BoardVisibility.PUBLIC,
                null,
                FIXED_TIME
            )
        );
        PageResponse<BoardSubscribeItemResponse> pageResponse = new PageResponse<>(
            responses,
            0,
            10,
            1,
            1,
            false,
            false
        );
        when(boardService.findSubscribes(eq(0), eq(10))).thenReturn(pageResponse);

        // When: 구독 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/boards/subscribes")
            .param("page", "0")
            .param("size", "10"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].boardId").value(1L));
    }

    // 게시판 게시글 목록 API는 응답 데이터를 반환해야 한다.
    @Test
    void findArticles_returns_list() throws Exception {
        // Given: 게시글 목록 응답
        List<ArticleSummaryResponse> pinned = List.of(
            new ArticleSummaryResponse(
                1L,
                10L,
                2L,
                "author",
                "notice",
                0L,
                0L,
                0L,
                0L,
                true,
                FIXED_TIME
            )
        );
        List<ArticleSummaryResponse> items = List.of(
            new ArticleSummaryResponse(
                2L,
                10L,
                2L,
                "author",
                "title",
                0L,
                0L,
                0L,
                0L,
                false,
                FIXED_TIME
            )
        );
        PageResponse<ArticleSummaryResponse> pageResponse = new PageResponse<>(
            items,
            0,
            10,
            1,
            1,
            false,
            false
        );
        BoardArticleListResponse response = new BoardArticleListResponse(pinned, pageResponse);
        when(articleService.getBoardArticles(10L, 0, 10, SortOrder.LATEST)).thenReturn(response);

        // When: 게시글 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/boards/10/articles")
            .param("page", "0")
            .param("size", "10")
            .param("order", "LATEST"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.pinned[0].id").value(1L))
            .andExpect(jsonPath("$.data.page.items[0].id").value(2L));
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

    // 게시판 이미지 업로드 API는 성공 응답을 반환해야 한다.
    @Test
    void uploadImage_returns_ok() throws Exception {
        // Given: 게시판 이미지 업로드 응답
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
        when(boardService.uploadBoardImage(eq(1L), any(), anyBoolean())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "boardImage",
            "board.png",
            "image/png",
            "data".getBytes()
        );

        // When: 게시판 이미지 업로드 API 호출
        ResultActions result = mockMvc.perform(multipart("/api/boards/1/image").file(file));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1L));
    }

    // 게시판 구독 API는 성공 응답을 반환해야 한다.
    @Test
    void subscribe_returns_ok() throws Exception {
        // Given: 구독 응답
        BoardSubscribeStatusResponse response = new BoardSubscribeStatusResponse(1L, true);
        when(boardService.subscribe(1L)).thenReturn(response);

        // When: 게시판 구독 API 호출
        ResultActions result = mockMvc.perform(post("/api/boards/1/subscribe"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subscribed").value(true));
    }

    // 게시판 구독 해제 API는 성공 응답을 반환해야 한다.
    @Test
    void unsubscribe_returns_ok() throws Exception {
        // Given: 구독 해제 응답
        BoardSubscribeStatusResponse response = new BoardSubscribeStatusResponse(1L, false);
        when(boardService.unsubscribe(1L)).thenReturn(response);

        // When: 게시판 구독 해제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/boards/1/subscribe"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subscribed").value(false));
    }

    // 게시판 가입 요청 API는 성공 응답을 반환해야 한다.
    @Test
    void requestJoin_returns_ok() throws Exception {
        // Given: 가입 요청 응답
        BoardMemberStatusResponse response = new BoardMemberStatusResponse(1L, BoardRole.PENDING);
        when(boardService.requestJoin(1L)).thenReturn(response);

        // When: 가입 요청 API 호출
        ResultActions result = mockMvc.perform(post("/api/boards/1/members"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberStatus").value("PENDING"));
    }

    // 게시판 가입 승인 API는 성공 응답을 반환해야 한다.
    @Test
    void approveJoin_returns_ok() throws Exception {
        // Given: 가입 승인 응답
        BoardMemberStatusResponse response = new BoardMemberStatusResponse(1L, BoardRole.MEMBER);
        when(boardService.approveJoin(1L, 2L)).thenReturn(response);

        // When: 가입 승인 API 호출
        ResultActions result = mockMvc.perform(post("/api/boards/1/members/2/approve"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberStatus").value("MEMBER"));
    }

    // 게시판 가입 취소 API는 성공 응답을 반환해야 한다.
    @Test
    void cancelJoin_returns_ok() throws Exception {
        // Given: 가입 취소 준비
        doNothing().when(boardService).cancelOwnMember(1L);

        // When: 가입 취소 API 호출
        ResultActions result = mockMvc.perform(delete("/api/boards/1/members/me"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }

    // 게시판 멤버 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void removeMember_returns_ok() throws Exception {
        // Given: 멤버 삭제 준비
        doNothing().when(boardService).cancelOrRejectMember(1L, 2L);

        // When: 멤버 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/boards/1/members/2"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
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
