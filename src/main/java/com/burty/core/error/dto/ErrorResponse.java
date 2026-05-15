package com.burty.core.error.dto;

import com.burty.core.error.enums.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Builder
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private int code;
    private String message;
    private String path;
    private String detail;

    public ErrorResponse(int code, String message, String path, String detail) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.detail = detail;
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                path,
                null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String detail) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                path,
                detail
        );
    }

    public static ErrorResponse of(int code, String message, String path) {
        return new ErrorResponse(code, message, path, null);
    }
}