package com.example.myblog.dto;

import com.example.myblog.domain.Article;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시글 생성/수정 요청 DTO
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequestDto {

    @NotBlank(message = "기사 제목(헤드라인)을 입력해주세요.")
    @Size(max = 200, message = "기사 제목은 최대 200자까지 입력 가능합니다.")
    private String title;   // 글 제목

    @NotBlank(message = "기사 본문을 입력해주세요.")
    private String content; // 글 본문

    // DTO -> Entity 변환 메서드
    public Article toEntity() {
        return Article.builder()
                .title(title)
                .content(content)
                .build();
    }
} //end ArticleRequestDto
