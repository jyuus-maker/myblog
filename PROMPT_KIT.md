# 🚀 바이브코더를 위한 AI 프롬프트 킷 (Prompt Kit for Vibe Coders)

> **"복사해서 붙여넣기만 하면 AI가 내 아이디어를 구현해 줍니다."**  
> 본 스타터팩(THE JOURNAL)은 이미 **Docker + PostgreSQL + Spring Boot 3.3 + JPA + Security + JWT + Thymeleaf + Nginx**가 100% 결합되어 있습니다.  
> 아래 프롬프트들을 **Cursor, Claude, ChatGPT, Gemini** 등에 그대로 복사해서 전달하면, 탄탄한 베이스 위에서 새로운 기능을 단 몇 초 만에 확장할 수 있습니다.

---

## 📑 프롬프트 목차
1. [프롬프트 1: 댓글(Comment) 기능 추가하기](#프롬프트-1-댓글comment-기능-추가하기)
2. [프롬프트 2: 카테고리(Category) / 태그(Tag) 분류 기능 추가하기](#프롬프트-2-카테고리category--태그tag-분류-기능-추가하기)
3. [프롬프트 3: 기사 실시간 검색(Search) 기능 추가하기](#프롬프트-3-기사-실시간-검색search-기능-추가하기)
4. [프롬프트 4: 신문 감성 디자인/폰트/색상 테마 커스텀하기](#프롬프트-4-신문-감성-디자인폰트색상-테마-커스텀하기)
5. [프롬프트 5: 내 프로젝트용 Jasypt 암호문 생성하기](#프롬프트-5-내-프로젝트용-jasypt-암호문-생성하기)

---

### 프롬프트 1: 댓글(Comment) 기능 추가하기

```markdown
지금 이 프로젝트는 Spring Boot 3.3 + JPA + PostgreSQL + Thymeleaf 기반의 블로그 스타터팩이야.
상세 기사 페이지(`article.html`) 하단에 로그인한 사용자가 댓글을 남길 수 있는 댓글(Comment) 기능을 추가해줘.

[요구사항]
1. Domain: `Comment` 엔티티 생성 (id, content, article(N:1), member(N:1), createdAt, modifiedAt)
2. DTO & Validation: `CommentRequestDto` 생성 및 `@NotBlank` 검증 적용
3. Service & Repository: `CommentService`, `CommentRepository` 구현
4. Controller:
   - `POST /api/articles/{articleId}/comments` (댓글 등록, 로그인 유저 연동)
   - `DELETE /api/comments/{commentId}` (본인 또는 관리자만 삭제 가능)
5. Frontend UI:
   - `article.html` 본문 하단에 에디토리얼 감성의 댓글 목록 및 작성 폼 추가
   - 비로그인 유저는 "로그인 후 댓글을 작성할 수 있습니다" 안내 모달 연동
   - `common/modal.js`의 `showToast()`와 `showConfirm()`을 활용해 등록/삭제 피드백 처리

기존 코드 스타일과 Bean Validation, GlobalExceptionHandler 규격을 그대로 유지해서 변경할 파일과 생성할 코드를 작성해줘.
```

---

### 프롬프트 2: 카테고리(Category) / 태그(Tag) 분류 기능 추가하기

```markdown
이 블로그 스타터팩의 기사(Article)에 카테고리(예: TECH, CULTURE, ECONOMY 등) 분류 기능을 추가하고 싶어.

[요구사항]
1. `Article` 엔티티에 `Category` (Enum 또는 독립 엔티티) 필드를 추가하고, 기존 12개 더미 데이터(data.sql)에도 카테고리를 부여해줘.
2. 기사 등록 폼(`newArticle.html`)의 툴바 상단에 카테고리 선택 드롭다운 UI를 추가해줘.
3. 메인 화면(`articleList.html`)의 신문 헤더 아래에 카테고리 네비게이션 탭을 만들어서, 탭 클릭 시 해당 카테고리 기사만 필터링되도록 해줘.
4. `BlogService`와 `BlogRepository`에 카테고리별 페이징 조회 메서드를 추가해줘.
```

---

### 프롬프트 3: 기사 실시간 검색(Search) 기능 추가하기

```markdown
메인 화면에서 제목과 본문 내용을 키워드로 검색할 수 있는 검색 기능을 추가해줘.

[요구사항]
1. `BlogRepository`에 `title` 또는 `content`에 검색어가 포함된 기사를 최신순으로 페이징 조회하는 메서드 추가 (`findByTitleContainingOrContentContaining...`)
2. `BlogViewController`의 `/articles` 엔드포인트에서 `keyword` 파라미터를 받을 수 있도록 수정
3. `articleList.html`의 신문 지면 헤더 우측에 미니멀한 검색창(Search Input) UI 배치
4. 검색어가 있을 때 지면 뷰 대신 검색 결과 목록을 깔끔하게 보여주고, 검색 결과가 없을 때의 감성적인 안내 문구 표시
```

---

### 프롬프트 4: 신문 감성 디자인/폰트/색상 테마 커스텀하기

```markdown
현재 `article.css`의 브로드시트 신문 테마를 [다크 모드 / 사이버펑크 에디션] (또는 [미니멀 북유럽 매거진 테마])로 변경하고 싶어.

[요구사항]
1. `:root`의 CSS 변수(`--bg-main`, `--text-primary`, `--accent-red` 등)를 테마에 어울리게 변경해줘.
2. 헤드라인 영문 폰트(Noto Serif KR -> Playfair Display 등)를 구글 폰트로 교체해줘.
3. 기존 2단 듀얼 글쓰기 데스크와 신문 조판 레이아웃의 비율과 가독성은 그대로 유지되도록 해줘.
```

---

### 프롬프트 5: 내 프로젝트용 Jasypt 암호문 생성하기

```markdown
현재 프로젝트의 `JasyptConfig` 설정을 참고해서, 내가 원하는 평문 비밀번호를 `ENC(...)` 암호문으로 변환해줘.

- 마스터 비밀키: `myblog-secret-key-1234`
- 암호화할 내 DB 비밀번호: `[여기에 본인의 DB 비밀번호 입력]`

이 비밀번호를 `PBEWithMD5AndDES` 알고리즘으로 암호화한 문자열과, `application-dev.properties`에 어떻게 교체해 넣으면 되는지 정확한 한 줄을 알려줘.
```

---

💡 **바이브코딩 꿀팁**:
- 코드를 추가할 때 AI에게 `"기존 src/main/resources/static/js/common/api.js의 httpRequest 함수를 사용해서 비동기 통신을 해줘"`라고 지시하면, 별도의 라이브러리 설치 없이 기존 JWT 토큰 인증과 자동 갱신 기능을 그대로 누릴 수 있습니다!
