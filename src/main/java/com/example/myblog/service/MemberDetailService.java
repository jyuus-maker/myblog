package com.example.myblog.service;

import com.example.myblog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 사용자 인증(로그인) 전용 서비스 클래스
 * - UserDetailsService 인터페이스를 구현하여 로그인 요청 시 DB에서 회원 정보를 조회합니다.
 */
@Service
@RequiredArgsConstructor
public class MemberDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 사용자 식별자(email)를 통해 사용자 정보를 조회합니다.
     * - Spring Security가 로그인 인증 처리 시 내부적으로 이 메서드를 호출합니다.
     *
     * @param email 로그인 폼에서 입력받은 사용자 이메일
     * @return 인증에 사용될 UserDetails 객체 (Member 엔티티)
     * @throws UsernameNotFoundException 해당 이메일의 회원이 존재하지 않을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }
}
