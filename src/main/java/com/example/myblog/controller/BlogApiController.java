package com.example.myblog.controller;

import com.example.myblog.domain.Article;
import com.example.myblog.dto.ArticleFeedItemResponseDto;
import com.example.myblog.dto.ArticleFeedPageResponseDto;
import com.example.myblog.dto.ArticleRequestDto;
import com.example.myblog.dto.ArticleResponseDto;
import com.example.myblog.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 블로그 REST API 컨트롤러
 * - 블로그 게시글의 CRUD(생성, 조회, 수정, 삭제) 및 피드 페이징 HTTP 요청을 처리합니다.
 */
@RestController
@RequiredArgsConstructor
public class BlogApiController {

    private final BlogService blogService;

    /**
     * 블로그 글 등록 API (POST /api/articles)
     * - 신규 리소스 생성 후 201 Created 상태 코드와 함께 생성된 자원의 위치(Location 헤더) 및 응답 DTO를 반환합니다.
     *
     * @param requestDto 생성할 게시글 데이터 (@Valid 적용)
     * @param uriBuilder URI 생성 도우미 빌더
     * @return 201 Created, Location 헤더 및 생성된 ArticleResponseDto
     */
    @PostMapping("/api/articles")
    public ResponseEntity<ArticleResponseDto> addArticle(@Valid @RequestBody ArticleRequestDto requestDto, UriComponentsBuilder uriBuilder) {
        Article savedArticle = blogService.save(requestDto);

        URI location = uriBuilder.path("/api/articles/{id}")
                .buildAndExpand(savedArticle.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(new ArticleResponseDto(savedArticle));
    } //end addArticle

    /**
     * 블로그 글 전체 목록 조회 API (GET /api/articles)
     * - 저장된 모든 게시글 목록을 응답 DTO 리스트로 변환하여 200 OK와 함께 반환합니다.
     *
     * @return 200 OK, 전체 ArticleResponseDto 목록
     */
    @GetMapping("/api/articles")
    public ResponseEntity<List<ArticleResponseDto>> findAllArticles() {
        List<ArticleResponseDto> articles = blogService.findAll()
                .stream()
                .map(ArticleResponseDto::new)
                .toList();

        return ResponseEntity.ok(articles);
    } //end findAllArticles

    /**
     * 블로그 피드 무한 스크롤용 페이징 API (GET /api/articles/feed?page=0&size=10)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (기본값 10)
     * @return 200 OK, ArticleFeedPageResponseDto
     */
    @GetMapping("/api/articles/feed")
    public ResponseEntity<ArticleFeedPageResponseDto> findArticlesFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleFeedItemResponseDto> pagedArticles = blogService.findAllPaged(pageable)
                .map(ArticleFeedItemResponseDto::new);

        return ResponseEntity.ok(new ArticleFeedPageResponseDto(pagedArticles));
    } //end findArticlesFeed

    /**
     * 블로그 글 단건(개별) 조회 API (GET /api/articles/{id})
     * - 주어진 ID에 해당하는 게시글을 조회하여 응답 DTO로 200 OK와 함께 반환합니다.
     *
     * @param id 조회할 게시글 ID
     * @return 200 OK, 단건 ArticleResponseDto
     */
    @GetMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponseDto> findArticle(@PathVariable("id") long id) {
        Article article = blogService.findById(id);

        return ResponseEntity.ok(new ArticleResponseDto(article));
    } //end findArticle

    /**
     * 블로그 글 수정 API (PUT /api/articles/{id})
     * - 주어진 ID의 게시글 제목과 본문을 수정한 후, 수정된 결과를 200 OK와 함께 반환합니다.
     *
     * @param id         수정할 게시글 ID
     * @param requestDto 수정할 제목 및 본문 데이터 (@Valid 적용)
     * @return 200 OK, 수정된 ArticleResponseDto
     */
    @PutMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponseDto> updateArticle(@PathVariable("id") long id, @Valid @RequestBody ArticleRequestDto requestDto) {
        Article updatedArticle = blogService.update(id, requestDto);

        return ResponseEntity.ok(new ArticleResponseDto(updatedArticle));
    } //end updateArticle

    /**
     * 블로그 글 삭제 API (DELETE /api/articles/{id})
     * - 주어진 ID의 게시글을 삭제하고 200 OK 상태 코드를 반환합니다. (본문 없음)
     *
     * @param id 삭제할 게시글 ID
     * @return 200 OK (Void)
     */
    @DeleteMapping("/api/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("id") long id) {
        blogService.delete(id);

        return ResponseEntity.ok()
                .build();
    } //end deleteArticle

} //end BlogApiController
