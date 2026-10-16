package com.example.myblog.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 커스텀 404 예외 클래스
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s을(를) 찾을 수 없습니다. (ID: %d)", resourceName, id));
    }
}
