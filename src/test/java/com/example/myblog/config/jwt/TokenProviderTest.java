package com.example.myblog.config.jwt;

import com.example.myblog.domain.Member;
import com.example.myblog.repository.MemberRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TokenProviderTest {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProperties jwtProperties;

    // =========================================================================
    // 1. 토큰 생성(generateToken) 테스트
    // =========================================================================
    @DisplayName("generateToken(): 유저 정보와 만료 기간을 전달해 토큰을 만들 수 있다.")
    @Test
    void generateToken() {
        // given : 테스트 유저 생성 및 DB 저장
        Member testMember = memberRepository.save(Member.builder()
                .email("user@gmail.com")
                .password("test1234")
                .build());

        // when : 14일 유효 기간의 토큰 생성 메서드 호출
        String token = tokenProvider.generateToken(testMember, Duration.ofDays(14));

        // then : 토큰을 파싱하여 클레임에 담긴 userId(PK)가 생성한 유저의 id와 일치하는지 검증
        Long memberId = Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);

        assertThat(memberId).isEqualTo(testMember.getId());
    }

    // =========================================================================
    // 2. 만료된 토큰 검증(validToken) 실패 테스트
    // =========================================================================
    @DisplayName("validToken(): 만료된 토큰인 경우에 유효성 검증에 실패한다.")
    @Test
    void validToken_invalidToken() {
        // given : 이미 만료된 시간(7일 전)으로 설정된 토큰 생성
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                .build()
                .createToken(jwtProperties);

        // when : 토큰 유효성 검증 메서드 호출
        boolean result = tokenProvider.validToken(token);

        // then : 검증 결과가 false인지 확인
        assertThat(result).isFalse();
    }

    // =========================================================================
    // 3. 유효한 토큰 검증(validToken) 성공 테스트
    // =========================================================================
    @DisplayName("validToken(): 유효한 토큰인 경우에 유효성 검증에 성공한다.")
    @Test
    void validToken_validToken() {
        // given : 현재로부터 14일 후 만료되는 유효한 토큰 생성
        String token = JwtFactory.withDefaultValues()
                .createToken(jwtProperties);

        // when : 토큰 유효성 검증 메서드 호출
        boolean result = tokenProvider.validToken(token);

        // then : 검증 결과가 true인지 확인
        assertThat(result).isTrue();
    }

    // =========================================================================
    // 4. 인증 정보 조회(getAuthentication) 테스트
    // =========================================================================
    @DisplayName("getAuthentication(): 토큰 기반으로 인증 정보를 가져올 수 있다.")
    @Test
    void getAuthentication() {
        // given : subject(email)가 포함된 토큰 생성
        String userEmail = "admin@gmail.com";
        String token = JwtFactory.builder()
                .subject(userEmail)
                .build()
                .createToken(jwtProperties);

        // when : 토큰으로부터 Authentication(인증 객체) 조회
        Authentication authentication = tokenProvider.getAuthentication(token);

        // then : 인증 객체의 principal(유저 정보)에서 email(username) 일치 여부 확인
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
    }

    // =========================================================================
    // 5. 유저 ID 추출(getMemberId) 테스트
    // =========================================================================
    @DisplayName("getMemberId(): 토큰으로 유저 ID를 가져올 수 있다.")
    @Test
    void getMemberId() {
        // given : 클레임에 id(PK) = 1L이 담긴 토큰 생성
        Long memberId = 1L;
        String token = JwtFactory.builder()
                .claims(Map.of("id", memberId))
                .build()
                .createToken(jwtProperties);

        // when : 토큰에서 memberId 추출 메서드 호출
        Long memberIdByToken = tokenProvider.getMemberId(token);

        // then : 추출된 ID가 given의 memberId(1L)와 일치하는지 확인
        assertThat(memberIdByToken).isEqualTo(memberId);
    }
}
