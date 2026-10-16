package com.example.myblog.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO
 * - 사용자로부터 입력받은 이메일과 비밀번호 데이터를 컨트롤러 및 서비스로 전달합니다.
 */
@Getter
@Setter
public class AddMemberRequestDto {
    private String email;
    private String password;
}
