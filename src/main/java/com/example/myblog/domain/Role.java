package com.example.myblog.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 권한(역할) Enum
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    ROLE_USER("일반 사용자"),
    ROLE_ADMIN("관리자");

    private final String description;
}
