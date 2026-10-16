package com.example.myblog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 회원(Member) 도메인 엔티티 클래스
 * - DB의 "members" 테이블과 매핑되는 JPA 엔티티입니다.
 * - Spring Security의 사용자 인증 및 권한 처리를 위해 UserDetails 인터페이스를 구현합니다.
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@EntityListeners(AuditingEntityListener.class)
public class Member implements UserDetails {

    /**
     * 회원 고유 식별 번호 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    /**
     * 로그인 아이디로 사용되는 회원 이메일
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * 암호화되어 저장되는 회원 비밀번호 (소셜 로그인인 경우 null 가능)
     */
    @Column(name = "password")
    private String password;

    /**
     * 사용자 닉네임/이름 (소셜 로그인 등에서 획득)
     */
    @Column(name = "nickname")
    private String nickname;

    /**
     * 회원 권한(역할) - 기본값: ROLE_USER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * 계정 생성일시 (JPA Auditing 자동 주입)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 수정일시 (JPA Auditing 자동 주입)
     */
    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    /**
     * 빌더 패턴 생성자
     */
    @Builder
    public Member(String email, String password, String nickname, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = (role != null) ? role : Role.ROLE_USER;
    }

    /**
     * 사용자 닉네임 수정
     */
    public Member update(String nickname) {
        this.nickname = nickname;
        return this;
    }

    /**
     * 사용자 권한 변경 (관리자용)
     */
    public Member updateRole(Role role) {
        this.role = role;
        return this;
    }

    // =========================================================================
    // Spring Security - UserDetails 인터페이스 필수 구현 메서드
    // =========================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role != null ? this.role.name() : Role.ROLE_USER.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
