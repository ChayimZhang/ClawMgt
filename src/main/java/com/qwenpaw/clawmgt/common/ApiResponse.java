package com.qwenpaw.clawmgt.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
