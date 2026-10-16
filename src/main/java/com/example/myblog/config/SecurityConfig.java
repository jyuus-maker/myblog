package com.example.myblog.config;

import com.example.myblog.config.jwt.ApiRequestHeaderFilter;
import com.example.myblog.config.jwt.JwtAuthenticationEntryPoint;
import com.example.myblog.config.jwt.TokenAuthenticationFilter;
import com.example.myblog.config.jwt.TokenProvider;
import com.example.myblog.config.oauth.OAuth2AuthorizationRequestBasedOnCookieRepository;
import com.example.myblog.config.oauth.OAuth2SuccessHandler;
import com.example.myblog.config.oauth.OAuth2UserCustomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final TokenProvider tokenProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 스프링 시큐리티 기능 비활성화(무시) 대상 설정
     */
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .requestMatchers(new AntPathRequestMatcher("/css/**"))
                .requestMatchers(new AntPathRequestMatcher("/js/**"))
                .requestMatchers(new AntPathRequestMatcher("/images/**"))
                .requestMatchers(new AntPathRequestMatcher("/uploads/**"));
    }

    /**
     * 비밀번호 암호화 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP 보안(SecurityFilterChain) 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CSRF, FormLogin 설정
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(formLoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .deleteCookies("access_token", "refresh_token", "JSESSIONID")
                        .invalidateHttpSession(true)
                )
                // 2. 헤더 필터 및 JWT 인증 필터 등록
                .addFilterBefore(apiRequestHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 3. 요청 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin", "/admin/**", "/api/admin", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/login", "/signup", "/user", "/api/token", "/uploads/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                // 4. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint.authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository))
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint.userService(oAuth2UserCustomService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // 5. 예외 및 권한 부족(403/401) 처리
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(customAccessDeniedHandler) // 403 발생 시 /articles?denied=true 로 자동 리다이렉트
                        .defaultAuthenticationEntryPointFor(
                                jwtAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .build();
    }

    @Bean
    public ApiRequestHeaderFilter apiRequestHeaderFilter() {
        return new ApiRequestHeaderFilter();
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }
}
