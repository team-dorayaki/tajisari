package com.tajisali.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tajisali.common.exception.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ApiError(errorCode.name(), errorCode.getMessage()));
    }
}
