package com.example.myblog.dto;

import com.example.myblog.domain.Article;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO
 */
@Getter
public class ArticleResponseDto {
    private final Long id; // 게시글 식별자
    private final String title; // 글 제목
    private final String content; // 글 본문
    private final long views; // 조회수
    private final LocalDateTime createdAt; // 작성일시

    // Entity -> DTO 변환 생성자
    public ArticleResponseDto(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.views = article.getViews();
        this.createdAt = article.getCreatedAt();
    }
} //end ArticleResponseDto
