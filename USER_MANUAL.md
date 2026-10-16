# 📖 THE JOURNAL — 사용자 및 배포 가이드 (User Manual)

본 가이드는 **THE JOURNAL** 스타터팩을 다운로드받아 자신의 프로젝트, 포트폴리오, 혹은 실제 운영 서비스로 구동하고자 하는 사용자를 위한 친절하고 상세한 매뉴얼입니다.

---

## 📑 목차
1. [스타터팩의 지향점 및 대상](#1-스타터팩의-지향점-및-대상)
2. [가장 쉬운 실행: 도커(Docker)로 1분 만에 띄우기 (권장)](#2-가장-쉬운-실행-도커docker로-1분-만에-띄우기-권장)
3. [로컬 개발 환경(PostgreSQL 직접 연동)에서 실행하기](#3-로컬-개발-환경postgresql-직접-연동에서-실행하기)
4. [Jasypt DB 비밀번호 암호화 커스터마이징 가이드](#4-jasypt-db-비밀번호-암호화-커스터마이징-가이드)
5. [Google 소셜 로그인 연동 가이드 (개발 완료 영역 & 사용자 할 일)](#5-google-소셜-로그인-연동-가이드-개발-완료-영역--사용자-할-일)
6. [프론트엔드 모듈 아키텍처 안내](#6-프론트엔드-모듈-아키텍처-안내)
7. [주요 기능 사용 팁](#7-주요-기능-사용-팁)
8. [실전 클라우드(AWS EC2, VPS 등) 배포 체크리스트](#8-실전-클라우드aws-ec2-vps-등-배포-체크리스트)
9. [자주 묻는 질문(FAQ) 및 트러블슈팅](#9-자주-묻는-질문faq-및-트러블슈팅)

---

## 1. 스타터팩의 지향점 및 대상

**THE JOURNAL**은 단순 "Hello World" 튜토리얼용 장난감이 아닙니다.  
**"실제 운영 데이터베이스(PostgreSQL)와 보안(Spring Security 6, JWT, Jasypt), 컨테이너 패키징(Docker + Nginx)까지 실전 배포 준비가 100% 끝난 상태에서, AI 도구(Cursor, Claude 등)와 함께 자신의 아이디어를 얹어보고 싶은 개발자/바이브코더"**를 위한 최고 수준의 베이스캠프입니다.

* 💡 **새로운 기능을 바로 붙여보고 싶다면?** 프로젝트 루트의 [PROMPT_KIT.md](PROMPT_KIT.md)를 열어 AI에게 복사해 붙여넣으세요!

---

## 2. 가장 쉬운 실행: 도커(Docker)로 1분 만에 띄우기 (권장)

이 스타터팩은 **PostgreSQL 16 DB + Spring Boot 3.3 백엔드 + Nginx 웹서버**가 Docker Compose로 완벽하게 묶여 있습니다.

### 단계 1: 프로젝트 다운로드 및 폴더 진입
```bash
cd springboot-starter-blogpack-v1.0
```

### 단계 2: 도커 컴포즈 실행
```bash
docker compose up -d --build
```
> **동작 원리:** PostgreSQL 컨테이너가 먼저 뜨고 `pg_isready` 헬스체크를 통과한 후 백엔드(Spring Boot)가 자동 기동되며, Nginx가 80 포트로 리버스 프록시를 수행합니다.

### 단계 3: 접속 확인
* **블로그 메인 지면:** `http://localhost`
* **관리자 회원 콘솔:** `http://localhost/admin/members` (기본 계정: `admin@myblog.com` / `admin1234`)

### 컨테이너 종료 및 재시작
```bash
# 서비스 종료
docker compose down

# 데이터까지 완전히 초기화하고 처음부터 다시 시작할 때
docker compose down -v
docker compose up -d --build
```

---

## 3. 로컬 개발 환경(PostgreSQL 직접 연동)에서 실행하기

로컬 PC에 설치된 PostgreSQL로 직접 실행하고 싶을 때의 가이드입니다.

### 단계 1: PostgreSQL 준비
로컬 PC의 PostgreSQL에 `blogdb` 데이터베이스를 생성합니다.
```sql
CREATE DATABASE blogdb;
```

### 단계 2: application-dev.properties 설정
`src/main/resources/application-dev.properties`를 열어 본인의 DB 접속 정보에 맞춥니다.
```properties
server.port=8090
spring.datasource.url=jdbc:postgresql://localhost:5432/blogdb
spring.datasource.username=postgres
# 암호화된 비밀번호를 쓰거나, 아래 4장의 가이드에 따라 평문/암호문으로 교체합니다.
spring.datasource.password=ENC(wqaY/ofx5yi9bC2IG46bQJJ3spjjoNRe)
```

### 단계 3: 실행
```bash
# Windows
.\gradlew.bat bootRun

# Mac/Linux
./gradlew bootRun
```
*브라우저 접속: `http://localhost:8090`*

---

## 4. Jasypt DB 비밀번호 암호화 커스터마이징 가이드

본 프로젝트는 깃허브 등에 코드를 올릴 때 DB 비밀번호가 노출되는 사고를 방지하기 위해 **Jasypt 암호화(`ENC(...)`)**가 기본 적용되어 있습니다.

### 내 DB 비밀번호로 암호문 만드는 2가지 방법

#### 방법 A: AI(Cursor/Claude)에게 시키기 (가장 추천)
AI에게 아래와 같이 요청하세요:
> *"Jasypt 알고리즘 `PBEWithMD5AndDES`와 마스터키 `myblog-secret-key-1234`를 사용해서 내 비밀번호 `[내비밀번호]`를 암호화한 ENC(...) 문자열을 만들어줘."*

#### 방법 B: 프로젝트 내 테스트 코드 활용하기
프로젝트에 내장된 `src/test/java/com/example/myblog/config/JasyptConfigTest.java`를 열어 원하는 비밀번호를 넣고 테스트를 실행(`Run`)하면 콘솔에 암호화된 `ENC(...)` 문자열이 출력됩니다.

그 출력값을 복사하여 `application-dev.properties`의 `spring.datasource.password`에 붙여넣기만 하면 끝납니다!

---

## 5. Google 소셜 로그인 연동 가이드 (개발 완료 영역 & 사용자 할 일)

본 스타터팩은 **Google OAuth2 소셜 로그인 백엔드 파이프라인이 이미 100% 개발 완료**되어 있습니다:
- `SecurityConfig`: OAuth2 로그인 활성화
- `OAuth2UserCustomService`: 소셜 로그인 사용자 자동 가입 및 DB 저장
- `OAuth2SuccessHandler`: 로그인 성공 시 JWT Access Token 발급 및 리다이렉트

### 사용자가 할 일 (Client ID / Secret 발급 후 30초 세팅)

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속하여 프로젝트를 생성합니다.
2. **API 및 서비스 > 사용자 인증 정보 > 사용자 인증 정보 만들기 > OAuth 클라이언트 ID**를 선택합니다.
3. 애플리케이션 유형을 **웹 애플리케이션**으로 선택합니다.
4. **승인된 리디렉션 URI**에 다음 주소를 추가합니다:
   * 로컬 개발: `http://localhost:8090/login/oauth2/code/google`
   * 도커/운영: `http://localhost/login/oauth2/code/google`
5. 발급된 **클라이언트 ID**와 **클라이언트 보안 비밀번호**를 복사하여, `.env` 파일 또는 환경변수에 넣습니다:
   ```properties
   GOOGLE_CLIENT_ID=발급받은_클라이언트_ID
   GOOGLE_CLIENT_SECRET=발급받은_보안_비밀번호
   ```
이제 로그인 페이지에서 **[GOOGLE PASSPORT]** 버튼을 누르면 구글 소셜 로그인이 즉시 연동됩니다.

---

## 6. 프론트엔드 모듈 아키텍처 안내

본 스타터팩의 자바스크립트는 1개의 거대한 파일이 아닌, **배움과 유지보수가 용이하도록 모듈화 분리**되어 있습니다:

```text
src/main/resources/static/js/
├── common/
│   ├── api.js           # JWT 토큰 관리, Fetch API 래퍼, 토큰 만료 시 자동 갱신(Replay)
│   └── modal.js         # alert/confirm을 대체하는 전용 에디토리얼 모달 & 토스트 알림
├── article/
│   ├── editor.js        # 듀얼 글쓰기 데스크(스플릿/위지윅/소스코드), 양방향 동기화, 인라인 폼 유효성 검증
│   ├── imageUploader.js # 캔버스 기반 자동 이미지 리사이징(1200px), 드래그&드롭, Ctrl+V 붙여넣기
│   └── feed.js          # 신문 모드 ON/OFF 스위치, IntersectionObserver 무한 스크롤, 독서 시간 계산
└── admin.js             # 관리자 콘솔 회원 권한 변경 및 계정 제어 로직
```

---

## 7. 주요 기능 사용 팁

* **신문 모드 ON / OFF**: 메인 지면 우측 상단의 슬라이드 스위치로 [전통 신문 6분할 조판]과 [최신순 매거진 피드]를 자유롭게 오갈 수 있습니다.
* **듀얼 글쓰기 데스크**: `/new-article`에서 [신문 편집자 모드 (2단 스플릿)]으로 마크다운을 치며 실시간 조판을 확인하거나, [위지윅 모드]에서 워드처럼 글을 작성할 수 있습니다.
* **드래그 & 드롭 이미지 최적화**: 10MB가 넘는 고용량 사진이라도 본문에 끌어다 놓으면 브라우저 캔버스에서 가로 1200px 수준으로 자동 경량화된 후 업로드됩니다.
* **Zero Alert/Confirm**: 브라우저 기본 경고창 대신 인라인 붉은 에러 문구와 감성적인 다이얼로그 모달이 피드백을 제공합니다.

---

## 8. 실전 클라우드(AWS EC2, VPS 등) 배포 체크리스트

1. **클라우드 서버 인스턴스 준비:** Ubuntu 22.04 LTS 권장
2. **도커 설치:**
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-v2
   sudo usermod -aG docker $USER
   ```
3. **방화벽(보안 그룹) 오픈:** `80 (HTTP)`, `443 (HTTPS)`
4. **저장소 복제 및 실행:**
   ```bash
   git clone https://github.com/jyuus-maker/springboot-starter-blogpack-v1.0.git
   cd springboot-starter-blogpack-v1.0
   docker compose up -d --build
   ```
5. **헬스체크 확인:**
   ```bash
   docker ps
   # journal-app 컨테이너가 (healthy) 상태인지 확인
   ```

---

## 9. 자주 묻는 질문(FAQ) 및 트러블슈팅

### Q1. 도커 실행 시 `port 5432 is already allocated` 에러가 납니다.
* **원인:** 로컬 PC에 설치된 PostgreSQL 서비스가 5432 포트를 선점 중이기 때문입니다.
* **해결법:** 로컬 PostgreSQL 서비스를 일시 중지하거나, `docker-compose.yml`에서 postgres의 포트 매핑을 `"5433:5432"`로 변경하세요.

### Q2. 테스트 코드는 어떻게 실행하나요?
```bash
./gradlew test
```
* JUnit 5와 Mockito를 통해 컨트롤러부터 서비스 비즈니스 로직(24시간 쿠키 중복 방지 조회수 증가 등)까지 격리된 H2 인메모리 DB에서 100% 안전하게 테스트를 수행합니다.

---

**THE JOURNAL과 함께 상상하던 멋진 웹 서비스를 현실로 만들어보세요!**
