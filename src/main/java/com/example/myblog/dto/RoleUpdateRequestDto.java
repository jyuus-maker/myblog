package com.example.myblog.dto;

import com.example.myblog.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 권한(Role) 변경 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class RoleUpdateRequestDto {
    private Role role;
}
