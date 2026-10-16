package com.example.myblog.controller;

import com.example.myblog.domain.Role;
import com.example.myblog.dto.MemberAdminResponseDto;
import com.example.myblog.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 어드민 관리자 화면(View) 컨트롤러
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final MemberService memberService;

    /**
     * 어드민 메인 접속 시 회원 관리 페이지로 리다이렉트 (/admin 및 /admin/ 모두 대응)
     */
    @GetMapping({"", "/"})
    public String adminRoot() {
        return "redirect:/admin/members";
    }

    /**
     * 어드민 회원 관리 대시보드 화면 (GET /admin/members)
     */
    @GetMapping("/members")
    public String memberList(Model model) {
        List<MemberAdminResponseDto> members = memberService.findAll()
                .stream()
                .map(MemberAdminResponseDto::new)
                .toList();

        model.addAttribute("members", members);
        model.addAttribute("totalCount", memberService.countTotal());
        model.addAttribute("adminCount", memberService.countByRole(Role.ROLE_ADMIN));
        model.addAttribute("userCount", memberService.countByRole(Role.ROLE_USER));

        return "admin/members";
    }
}
