package com.mocktalkback.global.common;

import lombok.Getter;

@Getter
public class ApiEnvelope<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiEnvelope(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, "OK", data);
    }

    public static ApiEnvelope<Void> ok() {
        return new ApiEnvelope<>(true, "OK", null);
    }

    public static ApiEnvelope<Void> fail(String message) {
        return new ApiEnvelope<>(false, message, null);
    }

    public static <T> ApiEnvelope<T> fail(String message, T data) {
        return new ApiEnvelope<>(false, message, data);
    }
}
