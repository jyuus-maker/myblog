package com.example.myblog.service;

import com.example.myblog.domain.Article;
import com.example.myblog.dto.ArticleRequestDto;
import com.example.myblog.exception.ResourceNotFoundException;
import com.example.myblog.repository.BlogRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlogService 비즈니스 로직 단위 테스트")
class BlogServiceTest {

    @InjectMocks
    private BlogService blogService;

    @Mock
    private BlogRepository blogRepository;

    @Nested
    @DisplayName("기사 생성 및 조회 테스트")
    class CrudTests {

        @Test
        @DisplayName("기사 등록 시 엔티티 변환 후 저장된다")
        void saveArticle() {
            // given
            ArticleRequestDto dto = new ArticleRequestDto("제목", "본문");
            Article savedArticle = Article.builder().title("제목").content("본문").build();
            given(blogRepository.save(any(Article.class))).willReturn(savedArticle);

            // when
            Article result = blogService.save(dto);

            // then
            assertThat(result.getTitle()).isEqualTo("제목");
            assertThat(result.getContent()).isEqualTo("본문");
            verify(blogRepository, times(1)).save(any(Article.class));
        }

        @Test
        @DisplayName("존재하지 않는 기사 ID 조회 시 ResourceNotFoundException이 발생한다")
        void findByIdNotFound() {
            // given
            given(blogRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> blogService.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("존재하지 않는 기사 ID 수정 시 ResourceNotFoundException이 발생한다")
        void updateNotFound() {
            // given
            given(blogRepository.findById(999L)).willReturn(Optional.empty());
            ArticleRequestDto updateDto = new ArticleRequestDto("새 제목", "새 본문");

            // when & then
            assertThatThrownBy(() -> blogService.update(999L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 기사 삭제 시 ResourceNotFoundException이 발생한다")
        void deleteNotFound() {
            // given
            given(blogRepository.existsById(999L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> blogService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("페이징 조회 시 Page 반환이 정상 동작한다")
        void findAllPaged() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Article> articles = List.of(
                    Article.builder().title("제목1").content("내용1").build(),
                    Article.builder().title("제목2").content("내용2").build()
            );
            Page<Article> page = new PageImpl<>(articles, pageable, 2);
            given(blogRepository.findAllByOrderByIdDesc(pageable)).willReturn(page);

            // when
            Page<Article> result = blogService.findAllPaged(pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목1");
        }
    }

    @Nested
    @DisplayName("24시간 쿠키 기반 조회수 증가 알고리즘 테스트")
    class ViewCountTests {

        @Test
        @DisplayName("최초 조회 시(쿠키 없음) 조회수가 1 증가하고 쿠키가 갱신된다")
        void increaseViewsFirstTime() {
            // given
            long articleId = 1L;
            Article article = Article.builder().title("제목").content("본문").views(0L).build();
            given(blogRepository.findById(articleId)).willReturn(Optional.of(article));

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            given(request.getCookies()).willReturn(null);

            // when
            Article result = blogService.findByIdAndIncreaseViews(articleId, request, response);

            // then
            assertThat(result.getViews()).isEqualTo(1L);
            verify(response, times(1)).addCookie(any(Cookie.class));
        }

        @Test
        @DisplayName("이미 쿠키에 [1]이 포함된 경우 조회수가 증가하지 않고 쿠키도 추가되지 않는다")
        void doNotIncreaseViewsIfAlreadyViewed() {
            // given
            long articleId = 1L;
            Article article = Article.builder().title("제목").content("본문").views(5L).build();
            given(blogRepository.findById(articleId)).willReturn(Optional.of(article));

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie viewCookie = new Cookie("viewed_articles", "[1][2]");
            given(request.getCookies()).willReturn(new Cookie[]{viewCookie});

            // when
            Article result = blogService.findByIdAndIncreaseViews(articleId, request, response);

            // then
            assertThat(result.getViews()).isEqualTo(5L);
            verify(response, never()).addCookie(any(Cookie.class));
        }
    }
}
