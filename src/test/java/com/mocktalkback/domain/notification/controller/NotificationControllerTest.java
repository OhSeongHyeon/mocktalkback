package com.mocktalkback.domain.notification.controller;

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
import com.mocktalkback.domain.notification.dto.NotificationCreateRequest;
import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.dto.NotificationUpdateRequest;
import com.mocktalkback.domain.notification.service.NotificationService;
import com.mocktalkback.domain.notification.type.NotificationType;
import com.mocktalkback.domain.notification.type.ReferenceType;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "DEV_SERVER_PORT=0"
})
class NotificationControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    // 알림 생성 API는 성공 응답을 반환해야 한다.
    @Test
    void create_returns_ok() throws Exception {
        // Given: 알림 생성 요청
        NotificationCreateRequest request = new NotificationCreateRequest(
            1L,
            2L,
            NotificationType.ARTICLE_COMMENT,
            "/boards/1/articles/1",
            ReferenceType.ARTICLE,
            10L,
            false
        );
        NotificationResponse response = new NotificationResponse(
            100L,
            1L,
            2L,
            NotificationType.ARTICLE_COMMENT,
            "/boards/1/articles/1",
            ReferenceType.ARTICLE,
            10L,
            false,
            FIXED_TIME,
            FIXED_TIME
        );
        when(notificationService.create(any(NotificationCreateRequest.class))).thenReturn(response);

        // When: 알림 생성 API 호출
        ResultActions result = mockMvc.perform(post("/api/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(100L));
    }

    // 알림 단건 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findById_returns_notification() throws Exception {
        // Given: 알림 응답
        NotificationResponse response = new NotificationResponse(
            100L,
            1L,
            2L,
            NotificationType.ARTICLE_COMMENT,
            "/boards/1/articles/1",
            ReferenceType.ARTICLE,
            10L,
            false,
            FIXED_TIME,
            FIXED_TIME
        );
        when(notificationService.findById(100L)).thenReturn(response);

        // When: 알림 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/notifications/100"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notiType").value("ARTICLE_COMMENT"));
    }

    // 알림 목록 조회 API는 리스트 응답을 반환해야 한다.
    @Test
    void findAll_returns_list() throws Exception {
        // Given: 알림 목록 응답
        List<NotificationResponse> responses = List.of(
            new NotificationResponse(
                100L,
                1L,
                2L,
                NotificationType.ARTICLE_COMMENT,
                "/boards/1/articles/1",
                ReferenceType.ARTICLE,
                10L,
                false,
                FIXED_TIME,
                FIXED_TIME
            ),
            new NotificationResponse(
                101L,
                1L,
                null,
                NotificationType.SYSTEM,
                "/",
                ReferenceType.USER,
                1L,
                false,
                FIXED_TIME,
                FIXED_TIME
            )
        );
        when(notificationService.findAll()).thenReturn(responses);

        // When: 알림 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/notifications"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(100L))
            .andExpect(jsonPath("$.data[1].id").value(101L));
    }

    // 알림 읽음 처리 API는 변경된 응답을 반환해야 한다.
    @Test
    void update_returns_updated_notification() throws Exception {
        // Given: 알림 읽음 처리 요청
        NotificationUpdateRequest request = new NotificationUpdateRequest(true);
        NotificationResponse response = new NotificationResponse(
            100L,
            1L,
            2L,
            NotificationType.ARTICLE_COMMENT,
            "/boards/1/articles/1",
            ReferenceType.ARTICLE,
            10L,
            true,
            FIXED_TIME,
            FIXED_TIME
        );
        when(notificationService.update(100L, request)).thenReturn(response);

        // When: 알림 수정 API 호출
        ResultActions result = mockMvc.perform(put("/api/notifications/100")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.read").value(true));
    }

    // 알림 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void delete_returns_ok() throws Exception {
        // Given: 알림 삭제 준비
        doNothing().when(notificationService).delete(100L);

        // When: 알림 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/notifications/100"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
