package com.example.myblog.config;

import com.example.myblog.config.jwt.TokenProvider;
import com.example.myblog.domain.Member;
import com.example.myblog.repository.MemberRepository;
import com.example.myblog.repository.RefreshTokenRepository;
import com.example.myblog.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

import static com.example.myblog.config.oauth.OAuth2SuccessHandler.ACCESS_TOKEN_COOKIE_NAME;
import static com.example.myblog.config.oauth.OAuth2SuccessHandler.REFRESH_TOKEN_COOKIE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofDays(1);

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String email = authentication.getName();
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        }

        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null) {
            // 1. JWT Access Token 발급 및 쿠키 저장
            String accessToken = tokenProvider.generateToken(member, ACCESS_TOKEN_DURATION);
            CookieUtil.deleteCookie(request, response, ACCESS_TOKEN_COOKIE_NAME);
            CookieUtil.addCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, (int) ACCESS_TOKEN_DURATION.toSeconds());

            // 2. JWT Refresh Token 발급 및 쿠키 저장
            String refreshToken = tokenProvider.generateToken(member, REFRESH_TOKEN_DURATION);
            CookieUtil.deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
            CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, (int) REFRESH_TOKEN_DURATION.toSeconds());

            log.info("[FormLoginSuccessHandler] JWT cookies issued for user: {}", email);
        }

        response.sendRedirect("/articles");
    }
}
