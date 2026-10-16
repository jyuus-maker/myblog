/**
 * =========================================================================
 * THE JOURNAL — Feed & Newspaper Mode Controller (feed.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. 신문 지면 모드(Broadsheet Mode: 상위 1~6위 큐레이션)와
 *    표준 매거진 피드 모드(Standard Feed: 최신순) 간의 실시간 토글을 제어합니다.
 * 2. 사용자의 선택 모드를 `localStorage`에 영구 보존하여 재방문 시에도 상태를 복원합니다.
 * 3. 매거진 피드에서는 `IntersectionObserver` API를 활용하여 사용자가 페이지 끝에 도달했을 때
 *    10건씩 다음 기사를 비동기 추가 로드(무한 스크롤)합니다.
 * 4. 기사 상세 페이지에서 본문 단어 수를 분석하여 "예상 독서 시간(EST. X MIN READ)"을 자동 계산합니다.
 */

document.addEventListener("DOMContentLoaded", function () {
    // -------------------------------------------------------------------------
    // 1. 관리자 권한 부족으로 튕겨져 나왔을 때의 알림 (?denied=true)
    // -------------------------------------------------------------------------
    const currentParams = new URLSearchParams(window.location.search);
    if (currentParams.get("denied") === "true") {
        showToast("🔒 관리자 전용 페이지입니다. 메인으로 이동되었습니다.", 4000);
        // 브라우저 히스토리에서 ?denied=true 쿼리스트링 제거
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // -------------------------------------------------------------------------
    // 2. 상단 네비게이션 로그인/비로그인 UI 분기
    // -------------------------------------------------------------------------
    const guestNav = document.querySelector("#guest-nav");
    const userNav = document.querySelector("#user-nav");
    const paperGuestNav = document.querySelector("#paper-guest-nav");
    const paperUserNav = document.querySelector("#paper-user-nav");
    const newArticleBtn = document.querySelector("#new-article-btn");
    const emptyWriteBtn = document.querySelector("#empty-write-btn");
    const logoutBtn = document.querySelector("#logout-btn");
    const paperLogoutBtn = document.querySelector("#paper-logout-btn");

    const loggedIn = isLoggedIn();
    if (loggedIn) {
        // 로그인 상태: [NEW PIECE], [SIGN OUT] 표시
        if (guestNav) guestNav.style.display = "none";
        if (userNav) userNav.style.display = "inline-flex";
        if (paperGuestNav) paperGuestNav.style.display = "none";
        if (paperUserNav) paperUserNav.style.display = "inline-flex";
    } else {
        // 비로그인 상태: [SIGN IN] 표시
        if (guestNav) guestNav.style.display = "inline-flex";
        if (userNav) userNav.style.display = "none";
        if (paperGuestNav) paperGuestNav.style.display = "inline-flex";
        if (paperUserNav) paperUserNav.style.display = "none";
    }

    // -------------------------------------------------------------------------
    // 3. 신문 모드(ON/OFF) 슬라이드 토글 스위치 제어
    // -------------------------------------------------------------------------
    const modeSwitches = document.querySelectorAll(".mode-switch-input");
    const paperToggleLabel = document.querySelector(".paper-toggle-label");
    const paperDateDisplay = document.querySelector("#paper-date-display");

    // 신문 상단 지면 날짜 자동 주입 (예: FRIDAY, OCT 16, 2026)
    if (paperDateDisplay) {
        const now = new Date();
        const days = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
        const months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];
        paperDateDisplay.textContent = `${days[now.getDay()]}, ${months[now.getMonth()]} ${now.getDate()}, ${now.getFullYear()}`;
    }

    /**
     * 신문 모드/표준 모드를 body 태그의 CSS 클래스로 전환하고 로컬스토리지에 저장
     * @param {boolean} isBroadsheet - true: 신문 지면 뷰, false: 표준 피드 뷰
     */
    function applyViewMode(isBroadsheet) {
        localStorage.setItem("article_view_mode", isBroadsheet ? "broadsheet" : "standard");

        if (isBroadsheet) {
            document.body.classList.add("broadsheet-mode");
            document.body.classList.remove("standard-mode");
            if (paperToggleLabel) paperToggleLabel.textContent = "신문 모드 ON";
        } else {
            document.body.classList.remove("broadsheet-mode");
            document.body.classList.add("standard-mode");
            if (paperToggleLabel) paperToggleLabel.textContent = "신문 모드 OFF";
        }

        // 화면 내 모든 토글 스위치 체크 상태 동기화
        modeSwitches.forEach(sw => {
            sw.checked = isBroadsheet;
        });
    }

    modeSwitches.forEach(sw => {
        sw.addEventListener("change", function () {
            applyViewMode(this.checked);
        });
    });

    // 이전에 저장된 모드를 불러오거나, 기본값으로 'broadsheet(신문 모드)' 적용
    const savedMode = localStorage.getItem("article_view_mode") || "broadsheet";
    applyViewMode(savedMode === "broadsheet");

    // -------------------------------------------------------------------------
    // 4. 매거진 연대기 피드 무한 스크롤 (IntersectionObserver)
    // -------------------------------------------------------------------------
    const feedContainer = document.querySelector("#article-feed-container");
    const sentinel = document.querySelector("#feed-infinite-sentinel");

    if (feedContainer && sentinel) {
        let isFeedLoading = false;
        const loader = sentinel.querySelector(".sentinel-loader");
        const endNotice = sentinel.querySelector(".sentinel-end");
        let feedObserver = null;

        /**
         * 다음 페이지의 기사 목록 10건을 비동기로 로드하여 리스트에 추가
         */
        const loadNextFeedPage = () => {
            const hasMore = sentinel.dataset.hasMore === "true";
            if (!hasMore || isFeedLoading) return;

            const nextPage = parseInt(sentinel.dataset.nextPage || "1", 10);
            isFeedLoading = true;
            if (loader) loader.style.display = "flex";
            if (endNotice) endNotice.style.display = "none";

            // 백엔드 피드 페이징 API 호출
            fetch(`/api/articles/feed?page=${nextPage}&size=10`)
                .then(res => {
                    if (!res.ok) throw new Error("Feed request failed");
                    return res.json();
                })
                .then(data => {
                    if (data && data.items && data.items.length > 0) {
                        data.items.forEach(item => {
                            const articleEl = document.createElement("article");
                            articleEl.className = "article-row";
                            articleEl.innerHTML = `
                                <div class="article-row-num">${item.formattedId}</div>
                                <div class="article-row-body">
                                    <h2 class="article-row-title">
                                        <a href="${item.detailUrl}" style="text-decoration:none; color:inherit;">${escapeHtml(item.title)}</a>
                                    </h2>
                                    <p class="article-row-excerpt">${escapeHtml(item.excerpt)}</p>
                                    <div>
                                        <a href="${item.detailUrl}" class="read-link">READ PIECE <span>→</span></a>
                                    </div>
                                </div>
                                <div class="article-row-meta">
                                    <span>${item.formattedDate}</span>
                                </div>
                            `;
                            feedContainer.appendChild(articleEl);
                        });
                    }

                    // 다음 페이지 존재 여부에 따라 감시 요소(sentinel) 상태 갱신
                    if (data.hasNext) {
                        sentinel.dataset.nextPage = (data.currentPage + 1).toString();
                        sentinel.dataset.hasMore = "true";
                        if (loader) loader.style.display = "none";
                    } else {
                        sentinel.dataset.hasMore = "false";
                        if (loader) loader.style.display = "none";
                        if (endNotice) endNotice.style.display = "flex";
                        if (feedObserver) feedObserver.disconnect(); // 관찰 중단
                    }
                })
                .catch(err => {
                    console.error("[Infinite Scroll Error]", err);
                    if (loader) loader.style.display = "none";
                })
                .finally(() => {
                    isFeedLoading = false;
                });
        };

        // 브라우저 뷰포트 하단 250px 근접 시 자동 트리거되도록 옵저버 등록
        const hasInitialMore = sentinel.dataset.hasMore === "true";
        if (hasInitialMore && "IntersectionObserver" in window) {
            feedObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        loadNextFeedPage();
                    }
                });
            }, {
                root: null,
                rootMargin: "250px",
                threshold: 0.1
            });

            feedObserver.observe(sentinel);
        }
    }

    // -------------------------------------------------------------------------
    // 5. 기사 작성 버튼 (인증 체크 & 커스텀 안내 모달)
    // -------------------------------------------------------------------------
    const handleWriteAction = () => {
        if (isLoggedIn()) {
            location.href = "/new-article";
        } else {
            showConfirm(
                "Authoring a piece requires authentication. Would you like to proceed to Sign In?",
                "AUTHORIZATION REQUIRED",
                () => { location.href = "/login"; }
            );
        }
    };

    if (newArticleBtn) newArticleBtn.addEventListener("click", handleWriteAction);
    if (emptyWriteBtn) emptyWriteBtn.addEventListener("click", handleWriteAction);

    // -------------------------------------------------------------------------
    // 6. 로그아웃 공통 처리
    // -------------------------------------------------------------------------
    const handleLogout = () => {
        localStorage.removeItem("access_token");
        deleteCookie("access_token");
        deleteCookie("refresh_token");
        showToast("LOGGED OUT OF DESK");
        setTimeout(() => { location.href = "/logout"; }, 500);
    };

    if (logoutBtn) logoutBtn.addEventListener("click", handleLogout);
    if (paperLogoutBtn) paperLogoutBtn.addEventListener("click", handleLogout);

    // -------------------------------------------------------------------------
    // 7. 기사 상세 화면 마크다운 렌더링 & 예상 독서 시간 계산
    // -------------------------------------------------------------------------
    const articleBodyEl = document.querySelector("#article-body-text");
    const readingTimeBadge = document.querySelector("#reading-time-badge");

    if (articleBodyEl) {
        const rawContent = (articleBodyEl.textContent || "").trim();
        
        // Marked.js 마크다운 라이브러리 렌더링
        if (typeof marked !== "undefined") {
            marked.setOptions({ breaks: true, gfm: true });
            // 이미 HTML 태그로 작성된 경우 원본 유지, 마크다운인 경우 파싱
            if (rawContent.startsWith("<") && (rawContent.includes("</p>") || rawContent.includes("</div>") || rawContent.includes("</h2>") || rawContent.includes("</blockquote>"))) {
                articleBodyEl.innerHTML = rawContent;
            } else {
                articleBodyEl.innerHTML = marked.parse(rawContent);
            }
        }

        // 성인 평균 분당 읽기 속도(약 180단어) 기준 독서 시간 계산
        if (readingTimeBadge) {
            const cleanText = articleBodyEl.innerText || "";
            const words = cleanText.trim() ? cleanText.trim().split(/\s+/).length : 0;
            const minutes = Math.max(1, Math.ceil(words / 180));
            readingTimeBadge.textContent = `EST. ${minutes} MIN READ`;
        }
    }
});
