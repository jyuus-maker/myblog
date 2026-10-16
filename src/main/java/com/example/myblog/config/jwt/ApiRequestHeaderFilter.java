package com.example.myblog.config.jwt;

import com.example.myblog.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * API 요청 헤더 검증 및 사전 가공 필터
 * - /api/** 경로 요청에 대해 헤더 유효성 검사, Request-ID 부여 및 로깅을 수행합니다.
 */
@Slf4j
public class ApiRequestHeaderFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // /api/** 경로에 대해서만 헤더 검증 및 추적 처리 수행
        if (requestURI.startsWith("/api")) {
            // 1. Request ID 설정 및 응답 헤더 추가 (요청 추적용)
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            response.setHeader(HEADER_REQUEST_ID, requestId);

            // 2. Authorization 헤더가 전달되었을 경우 형식 사전 검사
            String authHeader = request.getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                log.warn("[ApiRequestHeaderFilter] Invalid Authorization header format for URI: {}", requestURI);
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "INVALID_HEADER_FORMAT", "Authorization 헤더는 'Bearer <token>' 형식이어야 합니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(status, code, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
