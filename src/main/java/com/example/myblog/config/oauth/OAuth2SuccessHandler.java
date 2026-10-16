package com.example.myblog.config.oauth;

import com.example.myblog.config.jwt.TokenProvider;
import com.example.myblog.domain.Member;
import com.example.myblog.domain.RefreshToken;
import com.example.myblog.repository.MemberRepository;
import com.example.myblog.repository.RefreshTokenRepository;
import com.example.myblog.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

/**
 * OAuth2 소셜 로그인 성공 시 실행되는 핸들러
 * - 사용자 정보를 바탕으로 JWT(Access/Refresh Token)를 생성하고 쿠키에 저장한 뒤
 * - 대상 페이지(/articles)로 리다이렉트합니다.
 */
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofDays(1);
    public static final String REDIRECT_PATH = "/articles";

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestRepository;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected member email: " + email));

        // 1. Refresh Token 생성 -> DB 저장 -> 쿠키에 저장
        String refreshToken = tokenProvider.generateToken(member, REFRESH_TOKEN_DURATION);
        saveRefreshToken(member.getId(), refreshToken);
        addRefreshTokenToCookie(request, response, refreshToken);

        // 2. Access Token 생성 -> 쿠키에 저장
        String accessToken = tokenProvider.generateToken(member, ACCESS_TOKEN_DURATION);
        addAccessTokenToCookie(request, response, accessToken);

        // 3. 인증 관련 쿠키 및 세션 데이터 정리
        clearAuthenticationAttributes(request, response);

        // 4. 대상 경로(/articles)로 리다이렉트
        String targetUrl = getTargetUrl(accessToken);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * DB에 Refresh Token 저장 (기존 토큰이 있으면 갱신, 없으면 생성)
     */
    private void saveRefreshToken(Long memberId, String newRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByMemberId(memberId)
                .map(entity -> entity.update(newRefreshToken))
                .orElse(new RefreshToken(memberId, newRefreshToken));

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 생성된 Refresh Token을 HTTP 쿠키에 추가
     */
    private void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();
        CookieUtil.deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
        CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, cookieMaxAge);
    }

    /**
     * 생성된 Access Token을 HTTP 쿠키에 추가
     */
    private void addAccessTokenToCookie(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        int cookieMaxAge = (int) ACCESS_TOKEN_DURATION.toSeconds();
        CookieUtil.deleteCookie(request, response, ACCESS_TOKEN_COOKIE_NAME);
        CookieUtil.addCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, cookieMaxAge);
    }

    /**
     * OAuth2 인증 과정에서 임시 저장된 쿠키 제거
     */
    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    /**
     * 리다이렉트 대상 URL 생성 (필요에 따라 쿼리 파라미터로 토큰 전달 가능)
     */
    private String getTargetUrl(String token) {
        return UriComponentsBuilder.fromUriString(REDIRECT_PATH)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
