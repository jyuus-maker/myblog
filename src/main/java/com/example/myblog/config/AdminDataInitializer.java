package com.example.myblog.config;

import com.example.myblog.domain.Member;
import com.example.myblog.domain.Role;
import com.example.myblog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 기본 슈퍼 관리자(ADMIN) 및 일반 회원(USER) 계정을 초기화하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String DEFAULT_ADMIN_EMAIL = "admin@myblog.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin1234";

    public static final String DEFAULT_USER_EMAIL = "a@b.com";
    public static final String DEFAULT_USER_PASSWORD = "abc1234";

    @Value("${app.admin.email:admin@myblog.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin1234}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String targetAdminEmail = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : DEFAULT_ADMIN_EMAIL;
        String targetAdminPassword = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : DEFAULT_ADMIN_PASSWORD;

        // 1. 슈퍼 관리자 계정 초기화
        if (memberRepository.findByEmail(targetAdminEmail).isEmpty()) {
            Member admin = Member.builder()
                    .email(targetAdminEmail)
                    .password(passwordEncoder.encode(targetAdminPassword))
                    .nickname("슈퍼관리자")
                    .role(Role.ROLE_ADMIN)
                    .build();

            memberRepository.save(admin);
            log.info("⭐ 슈퍼 관리자(ADMIN) 계정이 자동 생성되었습니다: {}", targetAdminEmail);
        }

        // 2. 일반 사용자(USER) 계정 초기화 / 비밀번호 업데이트
        memberRepository.findByEmail(DEFAULT_USER_EMAIL).ifPresentOrElse(
                user -> {
                    // 기존 계정이 있으면 비밀번호를 abc1234로 갱신
                    Member updatedUser = Member.builder()
                            .email(DEFAULT_USER_EMAIL)
                            .password(passwordEncoder.encode(DEFAULT_USER_PASSWORD))
                            .nickname(user.getNickname() != null ? user.getNickname() : "일반필자")
                            .role(Role.ROLE_USER)
                            .build();
                    memberRepository.delete(user);
                    memberRepository.save(updatedUser);
                    log.info("⭐ 일반 회원(USER) 계정({})의 비밀번호가 '{}'로 갱신되었습니다.", DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD);
                },
                () -> {
                    Member user = Member.builder()
                            .email(DEFAULT_USER_EMAIL)
                            .password(passwordEncoder.encode(DEFAULT_USER_PASSWORD))
                            .nickname("일반필자")
                            .role(Role.ROLE_USER)
                            .build();
                    memberRepository.save(user);
                    log.info("⭐ 일반 회원(USER) 계정이 새로 생성되었습니다: {}", DEFAULT_USER_EMAIL);
                }
        );
    }
}
