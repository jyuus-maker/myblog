package com.example.myblog.service;

import com.example.myblog.domain.Article;
import com.example.myblog.dto.ArticleRequestDto;
import com.example.myblog.exception.ResourceNotFoundException;
import com.example.myblog.repository.BlogRepository;
import com.example.myblog.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 블로그 비즈니스 로직 처리 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션을 적용하여 성능 최적화
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private static final String VIEW_COOKIE_NAME = "viewed_articles";
    private static final int VIEW_COOKIE_EXPIRE_SECONDS = 60 * 60 * 24; // 24시간

    /**
     * 블로그 글 작성 (C)
     */
    @Transactional
    public Article save(ArticleRequestDto requestDto) {
        return blogRepository.save(requestDto.toEntity());
    }

    /**
     * 블로그 글 전체 목록 조회 (R) - 최신 글(ID 내림차순) 우선 정렬
     */
    public List<Article> findAll() {
        return blogRepository.findAllByOrderByIdDesc();
    }

    /**
     * 블로그 글 페이징 조회 (R) - 피드 무한 스크롤용 최신순 페이징
     */
    public Page<Article> findAllPaged(Pageable pageable) {
        return blogRepository.findAllByOrderByIdDesc(pageable);
    }

    /**
     * 전체 게시글 수 카운트
     */
    public long count() {
        return blogRepository.count();
    }

    /**
     * 블로그 글 전체 목록 조회 (R) - 조회수 높은 순(views 내림차순) 우선 정렬 (신문 지면 브로드시트 뷰용)
     */
    public List<Article> findAllByViews() {
        return blogRepository.findAllByOrderByViewsDescIdDesc();
    }

    /**
     * 블로그 글 단건(개별) 조회 (R)
     */
    public Article findById(long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", id));
    }

    /**
     * 블로그 글 상세 조회 + 조회수 1 증가 (24시간 쿠키 기반 중복 조회 방지)
     *
     * @param id       조회할 게시글 ID
     * @param request  HTTP 요청 (쿠키 검사용)
     * @param response HTTP 응답 (쿠키 갱신용)
     * @return 조회된 Article 엔티티
     */
    @Transactional
    public Article findByIdAndIncreaseViews(long id, HttpServletRequest request, HttpServletResponse response) {
        Article article = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", id));

        // 쿠키에서 이미 읽은 글 목록 조회 (형식: [1][5][12])
        String viewedArticlesCookie = null;
        if (request != null && request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (VIEW_COOKIE_NAME.equals(cookie.getName())) {
                    viewedArticlesCookie = cookie.getValue();
                    break;
                }
            }
        }

        String targetArticlePattern = "[" + id + "]";

        // 24시간 내에 읽은 적이 없는 경우에만 조회수 증가 및 쿠키 갱신
        if (viewedArticlesCookie == null || !viewedArticlesCookie.contains(targetArticlePattern)) {
            article.increaseViews();
            log.info("[BlogService] Issue #{} 조회수 증가 -> 현재 조회수: {}", id, article.getViews());

            if (response != null) {
                String updatedCookieVal = (viewedArticlesCookie == null ? "" : viewedArticlesCookie) + targetArticlePattern;
                CookieUtil.addCookie(response, VIEW_COOKIE_NAME, updatedCookieVal, VIEW_COOKIE_EXPIRE_SECONDS);
            }
        }

        return article;
    }

    /**
     * 블로그 글 수정 (U)
     */
    @Transactional
    public Article update(long id, ArticleRequestDto requestDto) {
        Article article = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", id));

        article.update(requestDto.getTitle(), requestDto.getContent());

        return article;
    }

    /**
     * 블로그 글 삭제 (D)
     */
    @Transactional
    public void delete(long id) {
        if (!blogRepository.existsById(id)) {
            throw new ResourceNotFoundException("게시글", id);
        }
        blogRepository.deleteById(id);
    }
}
