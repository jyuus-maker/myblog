package com.example.myblog.dto;

import com.example.myblog.domain.Article;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * 매거진 피드 무한 스크롤용 단일 아티클 항목 DTO
 */
@Getter
@NoArgsConstructor
public class ArticleFeedItemResponseDto {
    private Long id;
    private String formattedId;
    private String title;
    private String excerpt;
    private String formattedDate;
    private String detailUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public ArticleFeedItemResponseDto(Article article) {
        this.id = article.getId();
        this.formattedId = article.getId() < 10 ? "#0" + article.getId() : "#" + article.getId();
        this.title = article.getTitle();
        ArticleListViewResponseDto viewDto = new ArticleListViewResponseDto(article);
        this.excerpt = viewDto.getCleanExcerpt(160);
        this.formattedDate = article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FORMATTER) : "";
        this.detailUrl = "/articles/" + article.getId();
    }
}
