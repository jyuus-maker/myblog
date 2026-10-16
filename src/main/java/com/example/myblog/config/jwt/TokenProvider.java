package com.example.myblog.config.jwt;

import com.example.myblog.domain.Member;
import com.example.myblog.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * JWT(JSON Web Token) 생성, 유효성 검증, 인증 정보 추출을 담당하는 프로바이더 클래스
 */
@Service
@RequiredArgsConstructor
public class TokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * 회원 정보와 만료 시간을 기반으로 JWT 토큰을 생성합니다.
     *
     * @param member    토큰을 발급할 대상 회원 엔티티
     * @param expiredAt 토큰 유효 기간(Duration)
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Member member, Duration expiredAt) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), member);
    }

    /**
     * JWT 토큰의 실제 생성 로직
     *
     * @param expiry 만료 일시
     * @param member 회원 정보
     * @return 서명된 JWT 토큰 문자열
     */
    private String makeToken(Date expiry, Member member) {
        Date now = new Date();
        String roleStr = (member.getRole() != null) ? member.getRole().name() : Role.ROLE_USER.name();

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // 헤더 typ: JWT
                .setIssuer(jwtProperties.getIssuer())         // 내용 iss: 발급자
                .setIssuedAt(now)                             // 내용 iat: 발급 일시
                .setExpiration(expiry)                        // 내용 exp: 만료 일시
                .setSubject(member.getEmail())                // 내용 sub: 사용자 식별자(email)
                .claim("id", member.getId())                  // 클레임 id: 사용자 DB PK 번호
                .claim("role", roleStr)                       // 클레임 role: 사용자 권한
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey()) // 서명: 비밀키와 HS256 알고리즘
                .compact();
    }

    /**
     * JWT 토큰 유효성 검증
     * - 비밀키로 토큰을 복호화(파싱)하여 만료 여부 및 위변조를 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 경우 true, 만료/위변조 등의 경우 false
     */
    public boolean validToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey()) // 비밀키로 복호화 진행
                    .parseClaimsJws(token); // 토큰 파싱 및 서명 검증
            return true;
        } catch (Exception e) {
            // 복호화 과정에서 만료(ExpiredJwtException) 또는 서명 불일치 등의 예외 발생 시 유효하지 않은 토큰
            return false;
        }
    }

    /**
     * JWT 토큰으로부터 스프링 시큐리티 인증(Authentication) 객체를 생성합니다.
     *
     * @param token JWT 토큰
     * @return Authentication 객체 (UsernamePasswordAuthenticationToken)
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            role = Role.ROLE_USER.name();
        }
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(role));

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        claims.getSubject(),
                        "",
                        authorities
                ),
                token,
                authorities
        );
    }

    /**
     * JWT 토큰에서 사용자 고유 ID(PK)를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID (Long)
     */
    public Long getMemberId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }

    /**
     * JWT 토큰에서 사용자 권한(Role)을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 권한 문자열 (예: ROLE_USER, ROLE_ADMIN)
     */
    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * JWT 토큰을 파싱하여 클레임(Claims) 정보를 추출하는 내부 헬퍼 메서드
     *
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }
}
