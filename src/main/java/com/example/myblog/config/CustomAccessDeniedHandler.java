package com.example.myblog.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 일반 사용자가 관리자 전용 페이지(/admin/**) 등 인가되지 않은 리소스에 접근했을 때
 * Whitelabel 403 에러 페이지 대신 매거진 메인(/articles)으로 안전하게 리다이렉트시키는 핸들러
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        log.warn("[AccessDeniedHandler] 권한 없는 접근 감지: URI={}, 사용자={}", requestURI, request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");

        // API 요청인 경우 403 JSON 응답 반환
        if (requestURI.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"ACCESS_DENIED\",\"message\":\"접근 권한이 없습니다.\"}");
            return;
        }

        // 일반 웹 브라우저 페이지 요청인 경우 매거진 메인(/articles)으로 안전하게 리다이렉트 (알림 쿼리 파라미터 전달)
        response.sendRedirect("/articles?denied=true");
    }
}
