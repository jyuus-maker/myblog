package com.example.myblog.controller;

import com.example.myblog.dto.AddMemberRequestDto;
import com.example.myblog.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 회원 관련 화면(View) 및 회원가입/로그아웃 요청 처리 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 로그인 화면 이동 (GET /login)
     *
     * @return templates/login.html 뷰 반환
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 회원가입 화면 이동 (GET /signup)
     *
     * @return templates/signup.html 뷰 반환
     */
    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    /**
     * 회원가입 처리 (POST /user)
     * - signup.html의 폼에서 전송된 이메일/비밀번호로 신규 회원을 등록합니다.
     * - 가입 완료 후 로그인 화면(/login)으로 리다이렉트합니다.
     *
     * @param request 회원가입 요청 폼 데이터 (email, password)
     * @return 로그인 화면 리다이렉트 경로 ("redirect:/login")
     */
    @PostMapping("/user")
    public String signup(AddMemberRequestDto request) {
        memberService.save(request); // 회원가입 서비스 호출
        return "redirect:/login"; // 가입 성공 후 로그인 페이지로 이동
    }

    /**
     * 로그아웃 처리 (GET /logout)
     * - 현재 인증된 사용자의 세션 및 인증 컨텍스트를 클리어하고 로그아웃합니다.
     *
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 로그인 화면 리다이렉트 경로 ("redirect:/login")
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(
                request,
                response,
                SecurityContextHolder.getContext().getAuthentication()
        );
        return "redirect:/login";
    }
}
