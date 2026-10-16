package com.example.myblog.controller;

import com.example.myblog.domain.Article;
import com.example.myblog.dto.ArticleListViewResponseDto;
import com.example.myblog.service.BlogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 블로그 화면(HTML 뷰) 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class BlogViewController {

    private final BlogService blogService;

    // 루트 경로 접속 시 기사 목록(/articles)으로 리다이렉트 (GET /)
    @GetMapping("/")
    public String index() {
        return "redirect:/articles";
    }

    // 블로그 글 전체 목록 화면 (GET /articles)
    @GetMapping("/articles")
    public String getArticles(Model model) {
        // 1. 일반 매거진 피드 (초기 1페이지 10건 SSR 렌더링)
        Page<Article> articlePage = blogService.findAllPaged(PageRequest.of(0, 10));
        List<ArticleListViewResponseDto> articles = articlePage.getContent()
                .stream()
                .map(ArticleListViewResponseDto::new)
                .toList();

        // 2. 신문 브로드시트 지면용 (조회수 내림차순 상위 6건 큐레이션)
        List<ArticleListViewResponseDto> broadsheetArticles = blogService.findAllByViews()
                .stream()
                .limit(6)
                .map(ArticleListViewResponseDto::new)
                .toList();

        model.addAttribute("articles", articles);
        model.addAttribute("broadsheetArticles", broadsheetArticles);
        model.addAttribute("totalCount", articlePage.getTotalElements());
        model.addAttribute("hasNextPage", articlePage.hasNext());

        return "articleList";
    }

    // 블로그 글 상세(단건) 화면 (GET /articles/{id}) - 조회수 카운팅 연동
    @GetMapping("/articles/{id}")
    public String getArticle(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response, Model model) {
        // 조회수 1 증가 (24시간 중복 방지 쿠키 적용)
        Article article = blogService.findByIdAndIncreaseViews(id, request, response);
        model.addAttribute("article", article);

        return "article";
    }

    // 블로그 새 글 작성 및 수정 화면 (GET /new-article 또는 GET /new-article?id=1)
    @GetMapping("/new-article")
    public String newArticle(@RequestParam(required = false) Long id, Model model) {
        if (id == null) {
            model.addAttribute("article", new ArticleListViewResponseDto());
        } else {
            Article article = blogService.findById(id);
            model.addAttribute("article", new ArticleListViewResponseDto(article));
        }

        return "newArticle";
    }

} //end BlogViewController
