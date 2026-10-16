package com.example.myblog.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 매거진 피드 무한 스크롤용 페이징 응답 래퍼 DTO
 */
@Getter
@NoArgsConstructor
public class ArticleFeedPageResponseDto {
    private List<ArticleFeedItemResponseDto> items;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;

    public ArticleFeedPageResponseDto(Page<ArticleFeedItemResponseDto> page) {
        this.items = page.getContent();
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.hasNext = page.hasNext();
    }
}
