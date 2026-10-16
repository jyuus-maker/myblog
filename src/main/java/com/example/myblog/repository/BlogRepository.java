package com.example.myblog.repository;

import com.example.myblog.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 JPA 리포지토리 인터페이스
 */
@Repository
public interface BlogRepository extends JpaRepository<Article, Long> {

    // 최신 글(ID 내림차순) 순으로 전체 목록 조회
    List<Article> findAllByOrderByIdDesc();

    // 최신 글 페이징 조회 (피드 무한 스크롤용)
    Page<Article> findAllByOrderByIdDesc(Pageable pageable);

    // 조회수 높은 순(views 내림차순, ID 내림차순)으로 전체 목록 조회 (신문 지면 브로드시트 뷰용)
    List<Article> findAllByOrderByViewsDescIdDesc();

} //end BlogRepository
