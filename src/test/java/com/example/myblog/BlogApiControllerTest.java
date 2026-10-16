package com.example.myblog;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [BlogApiController 통합 테스트 클래스]
 * - 실제 스프링 부트 애플리케이션 컨텍스트를 로드하여 컨트롤러부터 리포지토리까지 전체 흐름을 테스트합니다.
 */
@SpringBootTest // 스프링 부트 애플리케이션 전체 컨텍스트(빈)를 로드하여 통합 테스트 환경을 구성합니다.
@AutoConfigureMockMvc // 컨트롤러 테스트를 위한 MockMvc 객체를 자동으로 구성하고 빈으로 등록합니다.
@TestPropertySource(locations = "classpath:application_test.properties") // 테스트 전용 프로퍼티 설정 파일을 적용합니다.
class BlogApiControllerTest {

    /**
     * MockMvc: 브라우저나 서버를 실제로 띄우지 않고도 스프링 MVC의 HTTP 요청(GET, POST 등)과 응답을 모의 테스트할 수 있는 객체입니다.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * ObjectMapper: Jackson 라이브러리의 핵심 객체로, Java 객체를 JSON 형태의 문자열로 변환(직렬화)하거나 그 반대로 변환(역직렬화)합니다.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * WebApplicationContext: 스프링의 웹 애플리케이션 컨텍스트(서블릿 컨텍스트 및 컨트롤러, 서비스 등의 빈 정보)를 담고 있습니다.
     */
    @Autowired
    private WebApplicationContext context;

    /**
     * BlogRepository: 테스트 전 DB 데이터 청소(초기화) 및 테스트 후 실제 데이터베이스에 정상 저장되었는지 검증하기 위해 주입받습니다.
     */
    @Autowired
    private BlogRepository blogRepository;

    /**
     * @BeforeEach: 각각의 테스트 메서드가 실행되기 직전에 매번 실행되는 초기화 메서드입니다.
     */
    @BeforeEach
    public void mockMvcSetUp() {
        // WebApplicationContext를 기반으로 MockMvc를 빌드하여 초기화합니다.
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
        // 이전 테스트의 잔여 데이터가 다음 테스트에 영향을 주지 않도록 테이블의 모든 데이터를 삭제(격리)합니다.
        blogRepository.deleteAll();
    } //end mockMvcSetUp

    /**
     * [블로그 글 추가(생성) API 테스트]
     * - Given: 클라이언트가 전송할 요청 DTO 객체를 만들고 JSON 문자열로 직렬화합니다.
     * - When: POST /api/articles 요청을 MockMvc를 통해 가상으로 전송합니다.
     * - Then: HTTP 응답 상태 코드가 201(Created)인지 검증하고, 실제 DB에서 데이터를 조회하여 일치하는지 확인합니다.
     */
    @DisplayName("addArticle: 블로그 글 등록에 성공한다.")
    @Test
    public void addArticle() throws Exception {
        // [1. Given: 테스트 환경 및 요청 데이터 준비]
        final String url = "/api/articles";
        final String title = "블로그 테스트 제목";
        final String content = "블로그 테스트 내용";
        final ArticleRequestDto userRequest = new ArticleRequestDto(title, content);

        // 자바 객체(ArticleRequestDto)를 JSON 형식의 문자열(String)로 변환
        final String requestBody = objectMapper.writeValueAsString(userRequest);

        // [2. When: 가상의 HTTP POST 요청 전송]
        // - url로 요청을 보내며, 요청 헤더의 Content-Type을 application/json으로 지정하고 requestBody를 전송합니다.
        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestBody));

        // [3. Then: 결과 응답 및 데이터 검증]
        // (1) 응답 상태 코드가 201 Created인지 검증
        result.andExpect(status().isCreated());

        // (2) 데이터베이스에서 전체 게시글 목록을 조회하여 실제로 저장되었는지 검증
        List<Article> articles = blogRepository.findAll();

        // 저장된 글의 총 개수가 1개인지 확인
        assertThat(articles.size()).isEqualTo(1);
        // 저장된 글의 제목이 요청한 제목과 일치하는지 확인
        assertThat(articles.get(0).getTitle()).isEqualTo(title);
        // 저장된 글의 본문이 요청한 본문과 일치하는지 확인
        assertThat(articles.get(0).getContent()).isEqualTo(content);
    } //end addArticle

    /**
     * [블로그 글 전체 목록 조회 API 테스트]
     * - Given: 테스트용 블로그 글(Article)을 미리 데이터베이스에 저장합니다.
     * - When: GET /api/articles 요청을 MockMvc를 통해 전송합니다.
     * - Then: HTTP 응답 상태 코드가 200(OK)인지 검증하고, 반환된 JSON 배열의 0번째 요소(title, content)가 저장한 데이터와 일치하는지 확인합니다.
     */
    @DisplayName("findAllArticles: 블로그 글 전체 목록 조회한다.")
    @Test
    public void findAllArticles() throws Exception {
        // [1. Given: 테스트 데이터 생성 및 저장]
        final String url = "/api/articles";
        final String title = "블로그 테스트 제목";
        final String content = "블로그 테스트 내용";

        // 데이터베이스에 테스트 데이터 1건 저장
        blogRepository.save(Article.builder()
                .title(title)
                .content(content)
                .build());

        // [2. When: 가상의 HTTP GET 요청 전송]
        // - GET /api/articles 요청을 보내며, Accept 헤더에 application/json을 지정합니다.
        final ResultActions resultActions = mockMvc.perform(
                get(url).accept(MediaType.APPLICATION_JSON)); // 받는 응답 데이터 형식

        // [3. Then: 응답 상태 코드 및 반환된 JSON 데이터 검증]
        resultActions
                // (1) HTTP 상태 코드가 200 OK인지 검증
                .andExpect(status().isOk())
                // (2) JSON 응답 배열의 첫 번째([0]) 객체의 title 값이 일치하는지 검증
                .andExpect(jsonPath("$[0].title").value(title))
                // (3) JSON 응답 배열의 첫 번째([0]) 객체의 content 값이 일치하는지 검증
                .andExpect(jsonPath("$[0].content").value(content));
    } //end findAllArticles

    /**
     * [블로그 글 단건(개별) 조회 API 테스트]
     * - Given: 테스트용 블로그 글(Article)을 미리 데이터베이스에 저장합니다.
     * - When: 저장된 글의 ID를 경로 변수({id})로 전달하여 GET /api/articles/{id} 요청을 전송합니다.
     * - Then: HTTP 응답 상태 코드가 200(OK)인지 검증하고, 반환된 JSON 객체의 title과 content가 저장한 데이터와 일치하는지 확인합니다.
     */
    @DisplayName("findArticle: 블로그 글 단건 조회에 성공한다.")
    @Test
    public void findArticle() throws Exception {
        // [1. Given: 테스트 데이터 생성 및 저장]
        final String url = "/api/articles/{id}";
        final String title = "블로그 테스트 제목";
        final String content = "블로그 테스트 내용";

        // 데이터베이스에 테스트 데이터 1건 저장
        Article savedArticle = blogRepository.save(Article.builder()
                .title(title)
                .content(content)
                .build());

        // [2. When: 가상의 HTTP GET 요청 전송 (/api/articles/{id})]
        // - url 템플릿의 {id} 자리에 저장된 게시글의 ID(savedArticle.getId())를 매핑하여 요청을 보냅니다.
        final ResultActions resultActions = mockMvc.perform(
                get(url, savedArticle.getId())
                        .accept(MediaType.APPLICATION_JSON)); // 받는 응답 데이터 형식 지정

        // [3. Then: 응답 상태 코드 및 반환된 단건 JSON 데이터 검증]
        resultActions
                // (1) HTTP 상태 코드가 200 OK인지 검증
                .andExpect(status().isOk())
                // (2) JSON 응답 단건 객체의 title 값이 일치하는지 검증
                .andExpect(jsonPath("$.title").value(title))
                // (3) JSON 응답 단건 객체의 content 값이 일치하는지 검증
                .andExpect(jsonPath("$.content").value(content));
    } //end findArticle

    /**
     * [블로그 글 수정 API 테스트]
     * - Given: 테스트용 블로그 글(Article)을 미리 데이터베이스에 저장하고, 수정할 제목과 내용을 담은 요청 DTO를 준비합니다.
     * - When: 저장된 글의 ID를 경로 변수({id})로 전달하여 PUT /api/articles/{id} 요청을 전송합니다.
     * - Then: HTTP 응답 상태 코드가 200(OK)인지 검증하고, 반환된 JSON 및 DB의 데이터가 수정 요청한 내용으로 정상 변경되었는지 확인합니다.
     */
    @DisplayName("updateArticle: 블로그 글 수정에 성공한다.")
    @Test
    public void updateArticle() throws Exception {
        // [1. Given: 테스트 데이터 생성 및 수정 요청 DTO 준비]
        final String url = "/api/articles/{id}";
        final String originalTitle = "원본 제목";
        final String originalContent = "원본 내용";

        // 기존 게시글 1건 저장
        Article savedArticle = blogRepository.save(Article.builder()
                .title(originalTitle)
                .content(originalContent)
                .build());

        // 수정할 데이터 준비 및 JSON 직렬화
        final String newTitle = "수정된 제목";
        final String newContent = "수정된 내용";
        final ArticleRequestDto requestDto = new ArticleRequestDto(newTitle, newContent);
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        // [2. When: 가상의 HTTP PUT 요청 전송 (/api/articles/{id})]
        // - url 템플릿의 {id} 자리에 저장된 게시글의 ID를 매핑하고 Content-Type 헤더와 함께 requestBody를 전송합니다.
        ResultActions resultActions = mockMvc.perform(
                put(url, savedArticle.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody));

        // [3. Then: 응답 상태 코드 및 수정된 데이터 검증]
        // (1) HTTP 상태 코드가 200 OK인지 검증
        resultActions
                .andExpect(status().isOk())
                // (2) JSON 응답 단건 객체의 title 값이 수정된 제목과 일치하는지 검증
                .andExpect(jsonPath("$.title").value(newTitle))
                // (3) JSON 응답 단건 객체의 content 값이 수정된 본문과 일치하는지 검증
                .andExpect(jsonPath("$.content").value(newContent));

        // (4) 데이터베이스에서 해당 글을 직접 조회하여 실제로 데이터가 수정되었는지 검증
        Article article = blogRepository.findById(savedArticle.getId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        assertThat(article.getTitle()).isEqualTo(newTitle);
        assertThat(article.getContent()).isEqualTo(newContent);
    } //end updateArticle

    /**
     * [블로그 글 삭제 API 테스트]
     * - Given: 테스트용 블로그 글(Article)을 미리 데이터베이스에 저장합니다.
     * - When: 저장된 글의 ID를 경로 변수({id})로 전달하여 DELETE /api/articles/{id} 요청을 전송합니다.
     * - Then: HTTP 응답 상태 코드가 200(OK)인지 검증하고, 데이터베이스에서 해당 글이 정상적으로 삭제되었는지(전체 글 목록이 비어있는지) 확인합니다.
     */
    @DisplayName("deleteArticle: 블로그 글 삭제에 성공한다.")
    @Test
    public void deleteArticle() throws Exception {
        // [1. Given: 테스트 데이터 생성 및 저장]
        final String url = "/api/articles/{id}";
        final String title = "블로그 테스트 제목";
        final String content = "블로그 테스트 내용";

        // 데이터베이스에 테스트 데이터 1건 저장
        Article savedArticle = blogRepository.save(Article.builder()
                .title(title)
                .content(content)
                .build());

        // [2. When: 가상의 HTTP DELETE 요청 전송 (/api/articles/{id})]
        // - url 템플릿의 {id} 자리에 저장된 게시글의 ID(savedArticle.getId())를 매핑하여 삭제 요청을 전송합니다.
        ResultActions resultActions = mockMvc.perform(
                delete(url, savedArticle.getId()));

        // [3. Then: 응답 상태 코드 및 데이터베이스 삭제 결과 검증]
        // (1) HTTP 응답 상태 코드가 200 OK인지 검증
        resultActions.andExpect(status().isOk());

        // (2) 데이터베이스에서 전체 게시글 목록을 조회하여 실제로 삭제되었는지(목록이 비어있는지) 검증
        List<Article> articles = blogRepository.findAll();

        // 삭제 후 저장된 글의 목록이 비어있는지(0개인지) 확인
        assertThat(articles).isEmpty();
    } //end deleteArticle

} //end BlogApiControllerTest

/*
 ====================================================================================================
                                [BlogApiControllerTest 테스트 코드 매뉴얼]
 ====================================================================================================

 1. 테스트 클래스 개요 및 목적
    - 본 테스트 클래스는 BlogApiController의 CRUD REST API 엔드포인트 전체 흐름을 검증하는 "통합 테스트(Integration Test)"입니다.
    - 실제 서블릿 컨테이너를 띄우지 않고 MockMvc를 통해 HTTP 요청/응답을 가상으로 생성하여, Controller -> Service -> Repository -> DB(H2)로
      이어지는 전체 계층 간의 유기적인 연동과 데이터 정합성을 신속하게 검증합니다.

 ----------------------------------------------------------------------------------------------------
 2. 핵심 어노테이션 및 의존성 설명
    (1) @SpringBootTest
        - 애플리케이션의 전체 Spring Context(모든 빈)를 로드하여 실제 운영 환경과 가장 유사한 상태로 통합 테스트를 수행합니다.
    (2) @AutoConfigureMockMvc
        - 컨트롤러 모의 테스트를 위한 핵심 객체인 MockMvc를 자동으로 구성하여 스프링 빈으로 등록합니다.
    (3) @TestPropertySource(locations = "classpath:application_test.properties")
        - 테스트 환경 전용 프로퍼티(H2 In-Memory DB, ddl-auto=create-drop 등)를 우선 적용하여 운영 DB에 영향을 주지 않도록 격리합니다.
    (4) @Autowired 주입 객체:
        - MockMvc: 모의 HTTP 요청(GET, POST, PUT, DELETE) 전송 및 응답 상태/데이터 검증
        - ObjectMapper: DTO 자바 객체 <-> JSON 문자열 간의 직렬화(Serialization) 및 역직렬화(Deserialization)
        - BlogRepository: 테스트 사전 데이터 셋업, 사후 DB 반영 여부 직접 조회/검증 및 데이터 초기화

 ----------------------------------------------------------------------------------------------------
 3. 테스트 데이터 격리 원칙 (@BeforeEach)
    - @BeforeEach public void mockMvcSetUp()
      -> 각 @Test 메서드가 실행되기 직전마다 항상 호출됩니다.
      -> blogRepository.deleteAll()을 실행하여 이전 테스트 메서드가 생성한 DB 데이터를 완벽히 제거함으로써,
         테스트 간 독립성(Isolation)과 재현 가능성(Repeatability)을 보장합니다.

 ----------------------------------------------------------------------------------------------------
 4. Given - When - Then 표준 테스트 패턴
    - [Given (준비)]: 테스트를 실행하기 위한 사전 데이터(Entity/DTO) 준비, JSON 직렬화, Mocking 등 환경 구성
    - [When (실행)]: MockMvc.perform()을 통해 대상 API의 HTTP 메서드(GET/POST/PUT/DELETE) 및 URL을 호출
    - [Then (검증)]: HTTP 상태 코드(200 OK, 201 Created 등), JSON 응답 본문 구조/값, DB 실제 데이터 상태 검증

 ----------------------------------------------------------------------------------------------------
 5. CRUD 엔드포인트별 테스트 시나리오 및 검증 항목
    ┌───────────────┬──────────────────────────┬─────────────────┬──────────────────────────────────────────┐
    │ HTTP 메서드   │ 엔드포인트 URL            │ 테스트 메서드명 │ 주요 검증 항목                           │
    ├───────────────┼──────────────────────────┼─────────────────┼──────────────────────────────────────────┤
    │ POST          │ /api/articles            │ addArticle      │ - HTTP 201 Created 상태 코드             │
    │               │                          │                 │ - DB에 신규 저장된 레코드 수(1건) 및 값  │
    ├───────────────┼──────────────────────────┼─────────────────┼──────────────────────────────────────────┤
    │ GET           │ /api/articles            │ findAllArticles │ - HTTP 200 OK 상태 코드                  │
    │               │                          │                 │ - JSON 배열 $[0].title, $[0].content 일치 │
    ├───────────────┼──────────────────────────┼─────────────────┼──────────────────────────────────────────┤
    │ GET           │ /api/articles/{id}       │ findArticle     │ - HTTP 200 OK 상태 코드                  │
    │               │                          │                 │ - JSON 단건 객체 $.title, $.content 일치 │
    ├───────────────┼──────────────────────────┼─────────────────┼──────────────────────────────────────────┤
    │ PUT           │ /api/articles/{id}       │ updateArticle   │ - HTTP 200 OK 상태 코드                  │
    │               │                          │                 │ - JSON 응답 내 수정된 title/content      │
    │               │                          │                 │ - DB 조회 시 실제 엔티티 수정 여부 검증  │
    ├───────────────┼──────────────────────────┼─────────────────┼──────────────────────────────────────────┤
    │ DELETE        │ /api/articles/{id}       │ deleteArticle   │ - HTTP 200 OK 상태 코드                  │
    │               │                          │                 │ - DB 조회 시 전체 목록이 비어있음(Empty) │
    └───────────────┴──────────────────────────┴─────────────────┴──────────────────────────────────────────┘

 ----------------------------------------------------------------------------------------------------
 6. MockMvc & JSONPath 문법 가이드
    - mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON_VALUE).content(jsonString))
      -> HTTP 요청 본문(Body)에 JSON 데이터를 실어서 POST 요청 전송
    - mockMvc.perform(get(url, id).accept(MediaType.APPLICATION_JSON))
      -> 경로 변수({id})를 바인딩하고 응답 데이터 타입을 JSON으로 지정하여 GET 요청 전송
    - andExpect(status().isCreated()) / andExpect(status().isOk())
      -> 응답 상태 코드가 각각 201 / 200 인지 단언(Assert)
    - andExpect(jsonPath("$.title").value("제목"))
      -> 단건 객체 응답의 'title' 속성 값 검증
    - andExpect(jsonPath("$[0].title").value("제목"))
      -> 배열 응답의 0번째 요소 객체의 'title' 속성 값 검증
 ====================================================================================================
 */
