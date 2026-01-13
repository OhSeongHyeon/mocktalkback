package com.mocktalkback.domain.file.controller;

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
import com.mocktalkback.domain.file.dto.FileCreateRequest;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.dto.FileUpdateRequest;
import com.mocktalkback.domain.file.service.FileService;

@WebMvcTest(controllers = FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "DEV_SERVER_PORT=0"
})
class FileControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    // 파일 생성 API는 성공 응답을 반환해야 한다.
    @Test
    void create_returns_ok() throws Exception {
        // Given: 파일 생성 요청
        FileCreateRequest request = new FileCreateRequest(
            1L,
            "photo.jpg",
            "uploads/2024/01/photo.jpg",
            1024L,
            "image/jpeg"
        );
        FileResponse response = new FileResponse(
            100L,
            1L,
            "photo.jpg",
            "uploads/2024/01/photo.jpg",
            1024L,
            "image/jpeg",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(fileService.create(any(FileCreateRequest.class))).thenReturn(response);

        // When: 파일 생성 API 호출
        ResultActions result = mockMvc.perform(post("/api/files")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(100L));
    }

    // 파일 단건 조회 API는 응답 데이터를 반환해야 한다.
    @Test
    void findById_returns_file() throws Exception {
        // Given: 파일 응답
        FileResponse response = new FileResponse(
            100L,
            1L,
            "photo.jpg",
            "uploads/2024/01/photo.jpg",
            1024L,
            "image/jpeg",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(fileService.findById(100L)).thenReturn(response);

        // When: 파일 조회 API 호출
        ResultActions result = mockMvc.perform(get("/api/files/100"));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.storageKey").value("uploads/2024/01/photo.jpg"));
    }

    // 파일 목록 조회 API는 리스트 응답을 반환해야 한다.
    @Test
    void findAll_returns_list() throws Exception {
        // Given: 파일 목록 응답
        List<FileResponse> responses = List.of(
            new FileResponse(
                100L,
                1L,
                "photo.jpg",
                "uploads/2024/01/photo.jpg",
                1024L,
                "image/jpeg",
                FIXED_TIME,
                FIXED_TIME,
                null
            ),
            new FileResponse(
                101L,
                1L,
                "doc.pdf",
                "uploads/2024/01/doc.pdf",
                2048L,
                "application/pdf",
                FIXED_TIME,
                FIXED_TIME,
                null
            )
        );
        when(fileService.findAll()).thenReturn(responses);

        // When: 파일 목록 API 호출
        ResultActions result = mockMvc.perform(get("/api/files"));

        // Then: 응답 리스트 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(100L))
            .andExpect(jsonPath("$.data[1].id").value(101L));
    }

    // 파일 수정 API는 변경된 응답을 반환해야 한다.
    @Test
    void update_returns_updated_file() throws Exception {
        // Given: 파일 수정 요청
        FileUpdateRequest request = new FileUpdateRequest(
            2L,
            "photo2.jpg",
            "uploads/2024/01/photo2.jpg",
            2048L,
            "image/jpeg"
        );
        FileResponse response = new FileResponse(
            100L,
            2L,
            "photo2.jpg",
            "uploads/2024/01/photo2.jpg",
            2048L,
            "image/jpeg",
            FIXED_TIME,
            FIXED_TIME,
            null
        );
        when(fileService.update(100L, request)).thenReturn(response);

        // When: 파일 수정 API 호출
        ResultActions result = mockMvc.perform(put("/api/files/100")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then: 응답 데이터 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fileName").value("photo2.jpg"));
    }

    // 파일 삭제 API는 성공 응답을 반환해야 한다.
    @Test
    void delete_returns_ok() throws Exception {
        // Given: 파일 삭제 준비
        doNothing().when(fileService).delete(100L);

        // When: 파일 삭제 API 호출
        ResultActions result = mockMvc.perform(delete("/api/files/100"));

        // Then: 성공 응답 확인
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
