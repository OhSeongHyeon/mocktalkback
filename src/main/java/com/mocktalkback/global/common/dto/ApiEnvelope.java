package com.mocktalkback.global.common.dto;

import lombok.Getter;

@Getter
public class ApiEnvelope<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;

    private ApiEnvelope(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, data, null);
    }

    public static ApiEnvelope<Void> ok() {
        return new ApiEnvelope<>(true, null, null);
    }

    public static ApiEnvelope<Void> fail(ApiError error) {
        return new ApiEnvelope<>(false, null, error);
    }
}
