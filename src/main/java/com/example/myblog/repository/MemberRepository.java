package com.example.myblog.repository;

import com.example.myblog.domain.Member;
import com.example.myblog.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회원(Member) 데이터 접근 레포지토리 인터페이스
 * - Spring Data JPA를 통해 "members" 테이블의 CRUD 및 쿼리 메서드를 제공합니다.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일(email)을 기준으로 회원 정보를 조회합니다.
     * - 로그인 시 사용자 식별 및 중복 가입 체크에 사용됩니다.
     *
     * @param email 조회할 회원 이메일
     * @return 조회된 Member 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    Optional<Member> findByEmail(String email);

    /**
     * 전체 회원을 최신 등록순(ID 내림차순)으로 조회합니다.
     */
    List<Member> findAllByOrderByIdDesc();

    /**
     * 특정 권한(Role)을 가진 회원 수를 조회합니다.
     */
    long countByRole(Role role);
}
