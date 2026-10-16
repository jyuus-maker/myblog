package com.example.myblog.config.jwt;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * JWT 테스트용 모의(Mock) 토큰 생성 팩토리 클래스
 * - 테스트 시 다양한 만료 시간 및 클레임을 가진 토큰을 유연하게 생성하기 위해 사용됩니다.
 */
@Getter
public class JwtFactory {

    private String subject = "test@email.com";
    private Date issuedAt = new Date();
    private Date expiration = new Date(new Date().getTime() + Duration.ofDays(14).toMillis());
    private Map<String, Object> claims = Collections.emptyMap();

    @Builder
    public JwtFactory(String subject, Date issuedAt, Date expiration, Map<String, Object> claims) {
        this.subject = subject != null ? subject : this.subject;
        this.issuedAt = issuedAt != null ? issuedAt : this.issuedAt;
        this.expiration = expiration != null ? expiration : this.expiration;
        this.claims = claims != null ? claims : this.claims;
    }

    /**
     * 기본 설정값으로 JwtFactory 인스턴스를 생성하는 팩토리 메서드
     */
    public static JwtFactory withDefaultValues() {
        return JwtFactory.builder().build();
    }

    /**
     * JwtProperties의 설정을 바탕으로 실제 서명된 JWT 토큰 문자열을 생성합니다.
     */
    public String createToken(JwtProperties jwtProperties) {
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .setSubject(subject)
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
                .compact();
    }
}
