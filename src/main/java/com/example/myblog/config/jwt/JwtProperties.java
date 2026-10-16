package com.example.myblog.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 관련 설정 프로퍼티 클래스
 * - application.properties의 jwt.issuer 및 jwt.secret_key 값을 바인딩합니다.
 */
@Component
@ConfigurationProperties("jwt")
@Getter
@Setter
public class JwtProperties {
    private String issuer = "myblog";
    private String secretKey = "study-springboot-jwt-secret-key-study-springboot-jwt-secret-key";
}
