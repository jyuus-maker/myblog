package com.example.myblog.exception;

import com.example.myblog.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("[GlobalExceptionHandler] ResourceNotFoundException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("[GlobalExceptionHandler] IllegalArgumentException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String defaultMessage = e.getBindingResult().getAllErrors().isEmpty()
                ? "잘못된 입력값입니다."
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.error("[GlobalExceptionHandler] MethodArgumentNotValidException: {}", defaultMessage);
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", defaultMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("[GlobalExceptionHandler] Unhandled Exception: ", e);
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
