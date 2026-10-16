package com.example.myblog.controller;

import com.example.myblog.dto.MemberAdminResponseDto;
import com.example.myblog.dto.RoleUpdateRequestDto;
import com.example.myblog.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 어드민 관리자 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final MemberService memberService;

    /**
     * 전체 회원 목록 JSON 조회 (GET /api/admin/members)
     */
    @GetMapping("/members")
    public ResponseEntity<List<MemberAdminResponseDto>> getAllMembers() {
        List<MemberAdminResponseDto> members = memberService.findAll()
                .stream()
                .map(MemberAdminResponseDto::new)
                .toList();

        return ResponseEntity.ok(members);
    }

    /**
     * 회원 권한(Role) 변경 API (PATCH /api/admin/members/{id}/role)
     */
    @PatchMapping("/members/{id}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable("id") Long id,
            @RequestBody RoleUpdateRequestDto request
    ) {
        if (request.getRole() == null) {
            return ResponseEntity.badRequest().body("변경할 권한(role)이 지정되지 않았습니다.");
        }

        try {
            memberService.updateRole(id, request.getRole());
            return ResponseEntity.ok("권한이 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 회원 삭제/강제 탈퇴 API (DELETE /api/admin/members/{id})
     */
    @DeleteMapping("/members/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable("id") Long id) {
        try {
            memberService.delete(id);
            return ResponseEntity.ok("회원이 정상적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
