# 📰 THE JOURNAL — Editorial Magazine & Blog Starter Pack (v1.0.0 Production Ready)

> **클래식 브로드시트(신문 1면) 조판 감성과 모던 엔터프라이즈 백엔드 아키텍처를 결합한 고품질 풀스택 블로그 스타터팩**  
> *100점 만점 기술 과제/배포 검수 완료 | Docker Ready | Bean Validation | JWT + OAuth2 + Form Login | Zero Alert/Confirm*

[![Java](https://img.shields.io/badge/Java-21%20(LTS)-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-blue?logo=docker)](https://www.docker.com/)
[![Nginx](https://img.shields.io/badge/Nginx-Alpine%20Proxy-009639?logo=nginx)](https://nginx.org/)
[![Database](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%206%20%2B%20JWT%20%2B%20OAuth2-red?logo=springsecurity)](https://spring.io/projects/spring-security)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

---

## 📌 프로젝트 소개 (Overview)

**THE JOURNAL**은 현대적인 웹 개발 기술 스택(Spring Boot 3.3.4, Java 21, Spring Security 6, JWT, OAuth2, PostgreSQL 16) 위에 정통 아날로그 인쇄 매체의 타이포그래피 미학을 결합한 에디토리얼 블로그/매거진 플랫폼입니다.

단순 튜토리얼용 코드가 아니라, **실제 프로덕션 배포 및 기업 기술과제 제출/교육용 스타터팩**으로 즉시 사용할 수 있도록 철저한 유효성 검증(Bean Validation), 공통 예외 표준화(400/404/500 JSON 규격), JPA 변경이력 추적(Auditing), 모달/인라인 피드백 UI, 그리고 **Docker Compose 기반 원클릭 오케스트레이션**이 완비되어 있습니다.

---

## ✨ 핵심 기능 (Key Features)

### 1. 🗞️ 브로드시트 신문 1면 6단계 활자 위계 조판 엔진
* **자동 큐레이션 신문 6분할 레이아웃**: 조회수 기반 1위~6위 기사를 인쇄 지면 스타일로 자동 렌더링.
  * **1위 메인 리드**: 2단 너비, 드롭캡(첫 글자 대형 활자) 및 보도사진 배치
  * **2위 서브 리드**: 본문 높이와 1:1 동기화되는 맞춤형 포토 프레임(`stretch` & `object-fit: cover`)
  * **3위 서브 스토리**: 우상단 정제된 활자율의 리드 기사
  * **4위~6위 브리핑/단신 기사**: 점프라인(`<[더보기] 란에서 이어짐 ☞>`) 및 메타데이터 지원
* **슬라이드 스위치**: [신문 1면 지면 뷰] ↔ [표준 매거진 연대기 피드] 간 실시간 모드 토글.
* **무한 스크롤 (Infinite Scroll)**: `IntersectionObserver` 기반 10건 단위 자동 페이징.

### 2. ✍️ 듀얼 글쓰기 데스크 (Dual Writing Desk) & 인라인 폼 피드백
* **신문 편집자 모드 (기본 2단 스플릿 뷰)**: 좌측 마크다운 입력 / 우측 신문 조판 실시간 프리뷰.
* **위지윅 모드**: 단일 캔버스 인플레이스 직접 편집.
* **`</>` 소스코드 보기**: HTML 원본 태그를 직접 확인 및 편집.
* **무손실 실시간 양방향 동기화**: 모드 전환 시 마크다운 ↔ HTML 무손실 자동 변환.
* **클라이언트 이미지 자동 최적화**: 캔버스 기반 자동 리사이징(최대 너비 1200px) 및 드래그&드롭/클립보드 붙여넣기(Ctrl+V) 지원.
* **Zero Alert/Confirm UX**: 브라우저 기본 경고창을 전면 퇴출하고, 붉은색 테두리/인라인 에러 문구 및 전용 에디토리얼 모달(`editorial-modal`) 탑재.

### 3. 🔒 엔터프라이즈 보안 및 견고한 백엔드 아키텍처
* **Bean Validation**: DTO 계층 `@NotBlank`, `@Size` 및 Controller `@Valid`를 통해 비정상 요청 사전 차단.
* **표준 공통 예외 처리**: `@RestControllerAdvice`로 `404 NOT_FOUND`(`ResourceNotFoundException`), `400 BAD_REQUEST`, `500 INTERNAL_SERVER_ERROR`를 일관된 JSON 스펙으로 반환.
* **JPA Auditing 전수 적용**: `Article`, `Member` 엔티티의 생성일시(`createdAt`), 수정일시(`modifiedAt`) 자동 추적.
* **JWT + OAuth2 + Form Login 삼중 인증**:
  * Access Token 만료 시 HttpOnly Refresh Token 기반 자동 재발급 & API Replay.
  * Google 소셜 로그인 지원.
* **관리자 전용 콘솔 (`/admin/**`)**: 권한 검증 및 회원 관리/권한 변경 기능.
* **Jasypt 암호화**: 민감 DB 패스워드 `ENC(...)` 안전 보관.

### 4. 🐳 완벽한 도커 및 클라우드 배포 패키징
* **멀티스테이지 `Dockerfile`**: Gradle 8.10 빌더 → Temurin JRE 21 Alpine 경량 런타임 분리로 이미지 용량 최소화.
* **Nginx 리버스 프록시 (`nginx.conf`)**: Gzip 실시간 압축, 100MB 업로드 허용, 이미지 캐싱(7일), 포트 80 단일 진입점 제공.
* **통합 `docker-compose.yml`**: PostgreSQL 16 헬스체크(`pg_isready`) 후 App 기동 보장, Named 볼륨 영속화.

---

## 🛠️ 기술 스택 (Tech Stack)

| 구분 | 기술 스택 |
| :--- | :--- |
| **Backend** | Java 21 (LTS), Spring Boot 3.3.4, Spring Data JPA, Spring Security 6, JWT, Jasypt |
| **Validation** | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| **Database** | PostgreSQL 16 (운영/도커), H2 Database (단위/통합 테스트 격리) |
| **Frontend** | Thymeleaf (SSR), Vanilla JavaScript (ES6+), Marked.js, Custom Editorial CSS |
| **DevOps / Infra** | Docker (Multi-stage), Docker Compose, Nginx (Alpine), HikariCP |
| **Testing** | JUnit 5, Mockito, MockMvc, AssertJ (전체 테스트 100% 통과) |

---

## 📁 디렉토리 구조 (Directory Structure)

```text
myblog/
├── Dockerfile                  # 멀티스테이지 백엔드 컨테이너 빌드 파일
├── docker-compose.yml          # DB + App + Nginx 원클릭 통합 배포 파일
├── USER_MANUAL.md              # 입문자/배포자를 위한 상세 사용 가이드
├── PROMPT_KIT.md               # 바이브코더를 위한 AI 프롬프트 킷
├── nginx/
│   └── nginx.conf              # 리버스 프록시 및 Gzip/캐싱 웹 서버 설정
├── src/
│   ├── main/
│   │   ├── java/com/example/myblog/
│   │   │   ├── config/         # Security, JWT, OAuth2, Jasypt 설정
│   │   │   ├── controller/     # REST API & SSR 뷰 컨트롤러
│   │   │   ├── domain/         # Article, Member, RefreshToken 엔티티
│   │   │   ├── dto/            # 요청/응답 DTO (Validation 적용)
│   │   │   ├── exception/      # ResourceNotFoundException & GlobalExceptionHandler
│   │   │   ├── repository/     # Spring Data JPA 리포지토리
│   │   │   └── service/        # BlogService (24h 쿠키 중복 방지), MemberService
│   │   └── resources/
│   │       ├── static/         # CSS, JS 모듈 자산 (인라인 에러, 모달, 무한스크롤, 이미지 리사이징)
│   │       ├── templates/      # Thymeleaf 신문/매거진/관리자 화면
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/                   # Controller MockMvc 및 Service 단위 테스트 슈트
├── build.gradle
└── .env.example
```

---

## 🚀 빠른 시작 (Quick Start)

### 방법 A: 도커로 1분 만에 실행하기 (권장)
사전에 Docker 및 Docker Compose가 설치되어 있어야 합니다.

```bash
# 1. 저장소 복제 및 디렉토리 이동
git clone https://github.com/jyuus-maker/springboot-starter-blogpack-v1.0.git
cd springboot-starter-blogpack-v1.0

# 2. 도커 컴포즈 빌드 및 실행
docker compose up -d --build

# 3. 브라우저 접속
# http://localhost (Nginx 포트 80으로 즉시 접속)
```

### 방법 B: 로컬 개발 환경에서 직접 실행하기
- Java 21 및 PostgreSQL 14+ 가 설치되어 있어야 합니다.

```bash
# 1. 테스트 실행 및 빌드 검증
./gradlew test

# 2. 로컬 서버 실행 (포트: 8090)
./gradlew bootRun
```
*브라우저 접속: `http://localhost:8090`*

---

## 🔑 기본 관리자 계정 (Initial Bootstrap)
- **이메일:** `admin@myblog.com`
- **비밀번호:** `admin1234`
- **관리자 콘솔 접속:** `http://localhost/admin/members` (도커 기준) 또는 `http://localhost:8090/admin/members` (로컬 기준)

---

## 📖 상세 매뉴얼 & 바이브코딩 프롬프트 킷 안내
- **설치, DB 암호화, 구글 로그인, 클라우드 배포 상세 가이드:** 루트 경로의 [USER_MANUAL.md](USER_MANUAL.md)
- **AI(Cursor/Claude)로 새 기능(댓글, 카테고리, 검색 등)을 30초 만에 붙이는 프롬프트:** 루트 경로의 [PROMPT_KIT.md](PROMPT_KIT.md)

---

## 📄 라이선스 (License)
본 프로젝트는 **MIT License**에 따라 자유롭게 수정, 배포, 상업적 이용 및 과제 제출용으로 활용하실 수 있습니다.
