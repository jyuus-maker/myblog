package com.example.myblog.service;

import com.example.myblog.config.AdminDataInitializer;
import com.example.myblog.domain.Member;
import com.example.myblog.domain.Role;
import com.example.myblog.dto.AddMemberRequestDto;
import com.example.myblog.repository.MemberRepository;
import com.example.myblog.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원(Member) 비즈니스 로직 처리 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 신규 회원 저장(일반 회원가입)
     * - 이메일 중복 여부를 검증하고, 비밀번호를 암호화하여 DB에 저장합니다.
     * - 기본 권한은 ROLE_USER로 등록됩니다.
     *
     * @param dto 회원가입 요청 데이터 (email, password)
     * @return 생성된 회원의 고유 ID (PK)
     * @throws IllegalArgumentException 이미 존재하는 이메일일 경우 예외 발생
     */
    @Transactional
    public Long save(AddMemberRequestDto dto) {
        // 1. 이메일 중복 검증
        if (memberRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + dto.getEmail());
        }

        // 2. 비밀번호 암호화 후 Member 엔티티 생성 및 DB 저장
        Member savedMember = memberRepository.save(
                Member.builder()
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword())) // BCrypt 암호화
                        .role(Role.ROLE_USER)
                        .build()
        );

        return savedMember.getId();
    }

    /**
     * 회원 ID로 회원 엔티티 조회
     *
     * @param memberId 회원 PK
     * @return 조회된 Member 엔티티
     */
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected member id: " + memberId));
    }

    /**
     * 전체 회원 목록 조회 (최신 가입순)
     */
    public List<Member> findAll() {
        return memberRepository.findAllByOrderByIdDesc();
    }

    /**
     * 회원 권한(Role) 변경
     *
     * @param memberId 대상 회원 ID
     * @param newRole  변경할 권한
     */
    @Transactional
    public void updateRole(Long memberId, Role newRole) {
        Member member = findById(memberId);

        // 기본 슈퍼 관리자 계정의 권한 강등 방지
        if (AdminDataInitializer.DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(member.getEmail()) && newRole != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("슈퍼 관리자 기본 계정의 권한은 변경할 수 없습니다.");
        }

        member.updateRole(newRole);
    }

    /**
     * 회원 삭제 (어드민 강제 탈퇴)
     *
     * @param memberId 삭제할 회원 ID
     */
    @Transactional
    public void delete(Long memberId) {
        Member member = findById(memberId);

        // 기본 슈퍼 관리자 계정 삭제 방지
        if (AdminDataInitializer.DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(member.getEmail())) {
            throw new IllegalArgumentException("슈퍼 관리자 계정은 삭제할 수 없습니다.");
        }

        // 연관된 RefreshToken 삭제
        refreshTokenRepository.findByMemberId(memberId).ifPresent(refreshTokenRepository::delete);

        // 회원 엔티티 삭제
        memberRepository.delete(member);
    }

    /**
     * 권한별 회원 수 카운트
     */
    public long countByRole(Role role) {
        return memberRepository.countByRole(role);
    }

    /**
     * 전체 회원 수
     */
    public long countTotal() {
        return memberRepository.count();
    }
}
