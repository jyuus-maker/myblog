/**
 * =========================================================================
 * THE JOURNAL — Toast & Confirmation Modal Module (modal.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. 브라우저의 딱딱하고 조악한 네이티브 경고창(alert, confirm)을 완전히 퇴출합니다.
 * 2. 에디토리얼 잡지/신문 콘셉트의 모던한 하단 플로팅 토스트(Toast)와
 *    중앙 오버레이 확인 다이얼로그(Modal)를 순수 바닐라 JS로 제공합니다.
 * 3. HTML 템플릿에 해당 요소가 없더라도 스크립트가 DOM에 동적으로 생성해 주므로,
 *    어느 페이지에서든 import만 하면 즉시 안전하게 작동합니다.
 */

/**
 * 사용자 피드백용 하단 토스트 메시지 출력
 * @param {string} message - 화면에 표시할 알림 텍스트
 * @param {number} duration - 토스트 유지 시간 (기본값: 3000ms = 3초)
 */
function showToast(message, duration = 3000) {
    // 1. 화면에 토스트 컨테이너가 존재하는지 확인하고, 없으면 동적 생성
    let toast = document.querySelector("#editorial-toast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "editorial-toast";
        document.body.appendChild(toast);
    }

    // 2. 메시지 주입 및 CSS 트랜지션 클래스(.show) 추가로 화면에 띄움
    toast.textContent = message;
    toast.classList.add("show");

    // 3. 지정된 시간이 지나면 .show 클래스를 제거하여 부드럽게 사라지게 처리
    setTimeout(() => {
        toast.classList.remove("show");
    }, duration);
}

/**
 * 비동기 콜백 기반 전용 커스텀 확인 모달 다이얼로그 (confirm 대체)
 * @param {string} message - 사용자에게 확인할 질문/경고 메시지
 * @param {string} metaTitle - 모달 상단에 작게 표시할 에디토리얼 카테고리/라벨
 * @param {Function} onConfirm - [CONTINUE / CONFIRM] 버튼 클릭 시 실행할 콜백 함수
 * @param {Function} [onCancel] - [CANCEL / DISMISS] 버튼 클릭 시 실행할 취소 콜백 함수 (선택)
 */
function showConfirm(message, metaTitle = "CONFIRMATION", onConfirm, onCancel) {
    // 1. 화면에 모달 오버레이가 이미 존재하는지 탐색, 없으면 동적 마크업 생성
    let modal = document.querySelector("#editorial-modal");
    if (!modal) {
        const wrapper = document.createElement("div");
        wrapper.id = "editorial-modal";
        wrapper.className = "editorial-modal-overlay";
        wrapper.innerHTML = `
            <div class="editorial-modal-box">
                <div class="modal-header-meta" id="modal-meta">${metaTitle}</div>
                <div class="modal-message" id="modal-message">${message}</div>
                <div class="modal-actions">
                    <button type="button" class="btn-editorial" id="modal-cancel-btn">CANCEL</button>
                    <button type="button" class="btn-editorial primary" id="modal-confirm-btn">CONTINUE</button>
                </div>
            </div>`;
        document.body.appendChild(wrapper);
        modal = wrapper;
    }

    // 2. 내부 텍스트 및 조작 버튼 요소 참조
    const metaEl = modal.querySelector("#modal-meta");
    const msgEl = modal.querySelector("#modal-message");
    const confirmBtn = modal.querySelector("#modal-confirm-btn");
    const cancelBtn = modal.querySelector("#modal-cancel-btn");

    if (metaEl) metaEl.textContent = metaTitle;
    if (msgEl) msgEl.textContent = message;

    // 3. 모달 활성화 (.active 클래스로 백드롭 블러 및 중앙 팝업 애니메이션 실행)
    modal.classList.add("active");

    // 4. 이벤트 중복 등록(Memory Leak) 방지를 위한 클린업 헬퍼 함수
    const cleanup = () => {
        if (confirmBtn) confirmBtn.removeEventListener("click", handleConfirm);
        if (cancelBtn) cancelBtn.removeEventListener("click", handleCancel);
    };

    // 5. 확인 클릭 시 처리 핸들러
    const handleConfirm = () => {
        modal.classList.remove("active");
        cleanup();
        if (onConfirm) onConfirm();
    };

    // 6. 취소 클릭 시 처리 핸들러
    const handleCancel = () => {
        modal.classList.remove("active");
        cleanup();
        if (onCancel) onCancel();
    };

    // 7. 버튼에 클릭 이벤트 등록
    if (confirmBtn) confirmBtn.addEventListener("click", handleConfirm);
    if (cancelBtn) cancelBtn.addEventListener("click", handleCancel);
}
