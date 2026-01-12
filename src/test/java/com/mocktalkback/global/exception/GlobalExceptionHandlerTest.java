package com.mocktalkback.global.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mocktalkback.global.common.dto.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(controllers = TestExceptionController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "DEV_SERVER_PORT=0")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    // 요청 바디 검증 실패 시 공통 에러 응답을 반환해야 한다.
    void method_argument_not_valid_returns_error_envelope() throws Exception {
        // Given
        var request = post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.error.reason").value(ErrorCode.COMMON_BAD_REQUEST.getDefaultMessage()))
                .andExpect(jsonPath("$.error.path").value("/test/validation"))
                .andExpect(jsonPath("$.error.details.fieldErrors").isArray())
                .andExpect(jsonPath("$.error.timestamp", notNullValue()));
    }

    @Test
    // 쿼리 파라미터 제약 위반 시 details에 violations가 포함되어야 한다.
    void constraint_violation_returns_error_details() throws Exception {
        // Given
        var request = get("/test/constraint")
                .param("count", "0");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.error.details.violations").isArray());
    }

    @Test
    // 경로 변수 타입 불일치 시 400과 상세 사유가 포함되어야 한다.
    void type_mismatch_returns_reason() throws Exception {
        // Given
        var request = get("/test/type/abc");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.error.reason").value("Invalid parameter: id"));
    }

    @Test
    // 지원하지 않는 HTTP 메서드는 405로 응답해야 한다.
    void method_not_allowed_returns_error_code() throws Exception {
        // Given
        var request = get("/test/method");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_METHOD_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.error.reason", containsString("GET")));
    }

    @Test
    // 인증 실패 시 401 에러 응답을 반환해야 한다.
    void authentication_exception_returns_unauthorized() throws Exception {
        // Given
        var request = get("/test/auth");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_UNAUTHORIZED.getCode()));
    }

    @Test
    // 권한 부족 시 403 에러 응답을 반환해야 한다.
    void access_denied_exception_returns_forbidden() throws Exception {
        // Given
        var request = get("/test/denied");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_FORBIDDEN.getCode()));
    }

    @Test
    // 잘못된 인자 예외는 400으로 매핑되어야 한다.
    void illegal_argument_returns_bad_request_reason() throws Exception {
        // Given
        var request = get("/test/illegal");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.error.reason").value("bad argument"));
    }

    @Test
    // 처리되지 않은 예외는 500으로 반환되어야 한다.
    void unhandled_exception_returns_internal_error() throws Exception {
        // Given
        var request = get("/test/runtime");

        // When
        var result = mockMvc.perform(request);

        // Then
        result
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.COMMON_INTERNAL_ERROR.getCode()));
    }

}
