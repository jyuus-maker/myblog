package com.example.myblog.controller;

import com.example.myblog.domain.Article;
import com.example.myblog.dto.ArticleRequestDto;
import com.example.myblog.repository.BlogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BlogApiController 테스트 클래스
 */
@SpringBootTest // 스프링 부트 통합 테스트 환경 로드
@AutoConfigureMockMvc // MockMvc 자동 설정 및 주입
class BlogApiControllerTest {

    @Autowired
    protected MockMvc mockMvc; // 모의 HTTP 요청 및 응답 테스트 객체

    @Autowired
    protected ObjectMapper objectMapper; // Java 객체 <-> JSON 변환기

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BlogRepository blogRepository;

    @BeforeEach
    public void mockMvcSetUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
        blogRepository.deleteAll(); // 테스트 격리를 위해 매 테스트 전 데이터 삭제
    }

    @DisplayName("addArticle: 블로그 글 추가에 성공한다.")
    @Test
    public void addArticle() throws Exception {
        // given: 게시글 등록 요청 객체 준비 및 JSON 변환
        final String url = "/api/articles";
        final String title = "테스트 제목";
        final String content = "테스트 내용";
        final ArticleRequestDto userRequest = new ArticleRequestDto(title, content);

        // 객체를 JSON 문자열로 직렬화
        final String requestBody = objectMapper.writeValueAsString(userRequest);

        // when: 블로그 글 등록 API 호출 (POST /api/articles)
        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestBody));

        // then: HTTP 상태 코드 201(Created) 확인 및 DB 데이터 검증
        result.andExpect(status().isCreated());

        List<Article> articles = blogRepository.findAll();

        assertThat(articles.size()).isEqualTo(1);
        assertThat(articles.get(0).getTitle()).isEqualTo(title);
        assertThat(articles.get(0).getContent()).isEqualTo(content);
    }

} //end BlogApiControllerTest
