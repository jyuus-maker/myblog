package com.example.myblog.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    @Builder
    public ErrorResponse(int status, String error, String code, String message) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(HttpStatus httpStatus, String message) {
        return ErrorResponse.builder()
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .code(httpStatus.name())
                .message(message)
                .build();
    }

    public static ErrorResponse of(HttpStatus httpStatus, String code, String message) {
        return ErrorResponse.builder()
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .code(code)
                .message(message)
                .build();
    }
}
