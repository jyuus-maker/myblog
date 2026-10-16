/**
 * =========================================================================
 * THE JOURNAL — Admin Console JavaScript Logic (admin.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. 관리자 전용 회원 관리 콘솔(/admin/members)의 프론트엔드 인터랙션을 담당합니다.
 * 2. 실시간 이메일/닉네임 검색 필터링을 제공합니다.
 * 3. 회원 권한 변경(ROLE_USER <-> ROLE_ADMIN) 및 계정 영구 삭제(강제 탈퇴) 시
 *    안전한 커스텀 확인 다이얼로그 모달을 띄워 실수를 방지합니다.
 * 4. API 호출 성공 시 DOM 요소를 동적으로 업데이트하여 화면 깜빡임 없이 부드러운 UX를 제공합니다.
 */

document.addEventListener('DOMContentLoaded', () => {

    // -------------------------------------------------------------------------
    // 1. 회원 실시간 검색 필터링 (이메일 및 닉네임)
    // -------------------------------------------------------------------------
    const searchInput = document.getElementById('member-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            const rows = document.querySelectorAll('.member-row');

            rows.forEach(row => {
                const email = row.getAttribute('data-email')?.toLowerCase() || '';
                const nickname = row.getAttribute('data-nickname')?.toLowerCase() || '';

                // 검색어가 포함된 행만 표시하고 나머지는 숨김 처리
                if (email.includes(query) || nickname.includes(query)) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // 2. 모달 및 토스트 알림 헬퍼
    // -------------------------------------------------------------------------
    const modal = document.getElementById('editorial-modal');
    const modalMessage = document.getElementById('modal-message');
    const modalMeta = document.getElementById('modal-meta');
    const modalConfirmBtn = document.getElementById('modal-confirm-btn');
    const modalCancelBtn = document.getElementById('modal-cancel-btn');
    const toast = document.getElementById('editorial-toast');

    let confirmCallback = null;

    function showToast(msg, duration = 3000) {
        if (!toast) return;
        toast.textContent = msg;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), duration);\n    }

    function showConfirmModal(meta, msg, onConfirm) {
        if (!modal) {
            const wrapper = document.createElement('div');
            wrapper.id = 'editorial-modal';
            wrapper.className = 'editorial-modal-overlay';
            wrapper.innerHTML = `
                <div class="editorial-modal-box">
                    <div class="modal-header-meta" id="modal-meta">${meta}</div>
                    <div class="modal-message" id="modal-message">${msg}</div>
                    <div class="modal-actions">
                        <button type="button" class="btn-editorial" id="modal-cancel-btn">CANCEL</button>
                        <button type="button" class="btn-editorial primary" id="modal-confirm-btn">CONTINUE</button>
                    </div>
                </div>`;
            document.body.appendChild(wrapper);
            wrapper.querySelector('#modal-cancel-btn').addEventListener('click', () => wrapper.classList.remove('active'));
            wrapper.querySelector('#modal-confirm-btn').addEventListener('click', () => {
                if (onConfirm) onConfirm();
                wrapper.classList.remove('active');
            });
            wrapper.classList.add('active');
            return;
        }

        modalMeta.textContent = meta;
        modalMessage.textContent = msg;
        confirmCallback = onConfirm;
        modal.classList.add('active');
    }

    function closeModal() {
        if (modal) modal.classList.remove('active');
        confirmCallback = null;
    }

    if (modalCancelBtn) modalCancelBtn.addEventListener('click', closeModal);
    if (modalConfirmBtn) {
        modalConfirmBtn.addEventListener('click', () => {
            if (confirmCallback) confirmCallback();
            closeModal();
        });
    }

    // -------------------------------------------------------------------------
    // 3. 회원 권한(Role) 변경 비동기 처리 (PATCH /api/admin/members/{id}/role)
    // -------------------------------------------------------------------------
    document.querySelectorAll('.role-select').forEach(select => {
        select.addEventListener('change', async (e) => {
            const memberId = e.target.getAttribute('data-member-id');
            const newRole = e.target.value;
            const originalRole = e.target.getAttribute('data-current-role');

            showConfirmModal(
                'ROLE MODIFICATION',
                `회원 (ID: ${memberId})의 권한을 ${newRole}로 변경하시겠습니까?`,
                async () => {
                    try {
                        const token = localStorage.getItem('access_token');
                        const headers = { 'Content-Type': 'application/json' };
                        if (token) headers['Authorization'] = `Bearer ${token}`;

                        const res = await fetch(`/api/admin/members/${memberId}/role`, {
                            method: 'PATCH',
                            headers: headers,
                            body: JSON.stringify({ role: newRole })
                        });

                        if (res.ok) {
                            showToast('권한이 성공적으로 변경되었습니다.');
                            e.target.setAttribute('data-current-role', newRole);
                            // 화면 배지(Badge) 업데이트
                            const badge = document.getElementById(`role-badge-${memberId}`);
                            if (badge) {
                                badge.textContent = newRole;
                                badge.className = `badge ${newRole === 'ROLE_ADMIN' ? 'badge-admin' : 'badge-user'}`;
                            }
                        } else {
                            const errText = await res.text();
                            showToast(errText || '권한 변경에 실패하였습니다.');
                            e.target.value = originalRole; // 이전 값으로 롤백
                        }
                    } catch (err) {
                        console.error(err);
                        showToast('오류가 발생했습니다.');
                        e.target.value = originalRole; // 이전 값으로 롤백
                    }
                }
            );
        });
    });

    // -------------------------------------------------------------------------
    // 4. 회원 영구 삭제(강제 탈퇴) 비동기 처리 (DELETE /api/admin/members/{id})
    // -------------------------------------------------------------------------
    document.querySelectorAll('.btn-delete-member').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const memberId = btn.getAttribute('data-member-id');
            const email = btn.getAttribute('data-member-email');

            showConfirmModal(
                'ACCOUNT EXPULSION',
                `[${email}] 회원을 완전히 삭제(강제 탈퇴)하시겠습니까? 이 작업은 되돌릴 수 없습니다.`,
                async () => {
                    try {
                        const token = localStorage.getItem('access_token');
                        const headers = { 'Content-Type': 'application/json' };
                        if (token) headers['Authorization'] = `Bearer ${token}`;

                        const res = await fetch(`/api/admin/members/${memberId}`, {
                            method: 'DELETE',
                            headers: headers
                        });

                        if (res.ok) {
                            showToast('회원이 정상적으로 삭제되었습니다.');
                            const row = document.getElementById(`member-row-${memberId}`);
                            if (row) {
                                row.remove();
                            }
                            // 상단 총 회원수 카운터 갱신 (-1)
                            const totalElem = document.getElementById('stat-total-count');
                            if (totalElem) {
                                const current = parseInt(totalElem.textContent, 10);
                                if (!isNaN(current) && current > 0) {
                                    totalElem.textContent = current - 1;
                                }
                            }
                        } else {
                            const errText = await res.text();
                            showToast(errText || '회원 삭제에 실패하였습니다.');
                        }
                    } catch (err) {
                        console.error(err);
                        showToast('오류가 발생했습니다.');
                    }
                }
            );
        });
    });

});
