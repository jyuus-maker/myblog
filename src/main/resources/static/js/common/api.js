/**
 * =========================================================================
 * THE JOURNAL — HTTP Request & JWT Token Engine (api.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. 프론트엔드의 모든 백엔드 REST API 비동기 통신(Fetch)을 일원화하여 관리합니다.
 * 2. 요청 시 localStorage 또는 쿠키에 보관된 JWT Access Token을 헤더(Authorization: Bearer ...)에
 *    자동으로 주입합니다.
 * 3. [핵심 보안 아키텍처: Automatic Token Refresh & API Replay]
 *    - 만약 요청 중 401(Unauthorized: Access Token 만료)이 발생하면 사용자에게 에러를 띄우지 않고,
 *      보안 쿠키의 Refresh Token으로 `/api/token`을 백그라운드 호출하여 새 Access Token을 재발급받은 뒤,
 *      원래 실패했던 사용자의 요청을 자동으로 다시 실행(Replay)합니다.
 * 4. Google 등 OAuth2 소셜 로그인 성공 후 쿼리스트링(?token=...)으로 유입된 토큰을 즉시 저장하고
 *    URL 주소창을 깨끗하게 정리(History Replace)합니다.
 */

/**
 * [즉시 실행 함수] 소셜 로그인 성공 후 리다이렉트된 URL에서 JWT Access Token 추출
 */
(function initTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get("token");
    if (tokenFromUrl) {
        // 1. 브라우저 로컬스토리지에 새 Access Token 보관
        localStorage.setItem("access_token", tokenFromUrl);
        // 2. URL 주소창에서 토큰 쿼리스트링(?token=...)을 지워 사용자 화면을 정돈하고 토큰 유출 방지
        const cleanUrl = window.location.pathname;
        window.history.replaceState({}, document.title, cleanUrl);
    }
})();

/**
 * 브라우저 쿠키(document.cookie)에서 특정 키의 값을 추출하는 헬퍼 함수
 * @param {string} key - 찾고자 하는 쿠키의 이름
 * @returns {string|null} 쿠키 값 또는 null
 */
function getCookie(key) {
    let result = null;
    const cookie = document.cookie.split(";");
    cookie.some(function (item) {
        item = item.trim();
        const dic = item.split("=");
        if (key === dic[0]) {
            result = decodeURIComponent(dic[1]);
            return true; // 탐색 종료
        }
    });
    return result;
}

/**
 * 특정 쿠키를 브라우저에서 만료시켜 삭제하는 헬퍼 함수
 * @param {string} name - 삭제할 쿠키 이름
 */
function deleteCookie(name) {
    document.cookie = name + "=; expires=Thu, 01 Jan 1970 00:00:01 GMT; path=/;";
}

/**
 * 현재 클라이언트가 로그인된 상태인지 확인 (Access Token 존재 여부)
 * @returns {boolean} 로그인 여부
 */
function isLoggedIn() {
    return !!(localStorage.getItem("access_token") || getCookie("access_token"));
}

/**
 * XSS 공격 방지를 위해 HTML 특수문자를 이스케이프하는 유틸리티
 * @param {string} str - 원본 문자열
 * @returns {string} 안전하게 치환된 문자열
 */
function escapeHtml(str) {
    if (!str) return "";
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * JWT 인증 및 토큰 자동 갱신(Replay)이 포함된 공통 Fetch API 래퍼
 * @param {string} method - HTTP 메서드 ("GET", "POST", "PUT", "DELETE" 등)
 * @param {string} url - 요청할 백엔드 API 엔드포인트 URL
 * @param {string|null} body - 요청 바디 JSON 문자열 (GET인 경우 null)
 * @param {Function} success - 성공 시 응답 객체를 전달받는 콜백
 * @param {Function} fail - 최종 실패 시 응답 객체를 전달받는 콜백
 */
function httpRequest(method, url, body, success, fail) {
    // 1. 현재 저장된 Access Token 조회
    const accessToken = localStorage.getItem("access_token") || getCookie("access_token");

    // 2. Fetch API 전송 (헤더에 Bearer 토큰 주입)
    fetch(url, {
        method: method,
        headers: {
            Authorization: accessToken ? `Bearer ${accessToken}` : "",
            "Content-Type": "application/json",
        },
        body: body,
    }).then(response => {
        // [케이스 1] 요청 성공 (200 OK 또는 201 Created)
        if (response.status === 200 || response.status === 201) {
            return success(response);
        }

        // [케이스 2] 401 Unauthorized (Access Token 만료) 발생 시 자동 갱신 파이프라인 가동
        const refreshToken = getCookie("refresh_token");
        if (response.status === 401 && refreshToken) {
            // 백엔드의 토큰 재발급 API(/api/token)에 Refresh Token 전송
            fetch("/api/token", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken: refreshToken }),
            })
                .then(res => {
                    if (res.ok) return res.json();
                })
                .then(result => {
                    if (result && result.accessToken) {
                        // 새 Access Token을 저장하고, 이전에 실패했던 본래 요청을 똑같이 재실행(Replay)
                        localStorage.setItem("access_token", result.accessToken);
                        httpRequest(method, url, body, success, fail);
                    } else {
                        // Refresh Token마저 만료된 경우 세션 종료 및 로그인 화면 이동
                        showToast("SESSION EXPIRED. PLEASE SIGN IN.");
                        setTimeout(() => location.replace("/login"), 1000);
                    }
                })
                .catch(() => {
                    showToast("AUTHENTICATION ERROR.");
                    setTimeout(() => location.replace("/login"), 1000);
                });
        } else {
            // [케이스 3] 400 Bad Request, 403 Forbidden, 404 Not Found 등 기타 실패 응답
            return fail(response);
        }
    });
}
