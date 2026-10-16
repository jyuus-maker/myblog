package com.example.myblog.dto;

import com.example.myblog.domain.Member;
import com.example.myblog.domain.Role;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 어드민 회원 목록 및 상세 조회용 응답 DTO
 */
@Getter
public class MemberAdminResponseDto {

    private final Long id;
    private final String email;
    private final String nickname;
    private final Role role;
    private final boolean isSocial;
    private final LocalDateTime createdAt;

    public MemberAdminResponseDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getNickname();
        this.role = member.getRole();
        // 비밀번호가 없으면 소셜(OAuth2) 가입 계정
        this.isSocial = (member.getPassword() == null || member.getPassword().isBlank());
        this.createdAt = member.getCreatedAt();
    }
}
