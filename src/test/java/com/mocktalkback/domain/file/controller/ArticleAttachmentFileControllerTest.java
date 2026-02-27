package com.mocktalkback.domain.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.service.ArticleAttachmentFileService;

@WebMvcTest(controllers = ArticleAttachmentFileController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "SERVER_PORT=0"
})
class ArticleAttachmentFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleAttachmentFileService articleAttachmentFileService;

    // 게시글 첨부파일 업로드 API는 업로드 결과를 응답해야 한다.
    @Test
    void uploadArticleAttachmentFile_returns_uploaded_file() throws Exception {
        // Given: 업로드 응답 데이터
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file",
            "guide.txt",
            "text/plain",
            "attachment".getBytes(StandardCharsets.UTF_8)
        );
        FileResponse response = new FileResponse(
            10L,
            1L,
            "guide.txt",
            "/uploads/guide.txt",
            10L,
            "text/plain",
            null,
            null,
            null
        );
        when(articleAttachmentFileService.uploadArticleAttachmentFile(any(MockMultipartFile.class), eq(false)))
            .thenReturn(response);

        // When: 첨부파일 업로드 API를 호출하면
        ResultActions result = mockMvc.perform(
            multipart("/api/files/article-attachments")
                .file(multipartFile)
        );

        // Then: 업로드 파일 정보가 반환된다.
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.fileName").value("guide.txt"))
            .andExpect(jsonPath("$.data.mimeType").value("text/plain"));
    }
}
