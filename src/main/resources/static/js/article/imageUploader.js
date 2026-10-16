/**
 * =========================================================================
 * THE JOURNAL — Client Image Resizing & Upload Pipeline (imageUploader.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. 고화질 보도사진/스크린샷 업로드 시 원본 크기(예: 4K, 10MB+)를 그대로 서버에 전송하면
 *    서버 네트워크 트래픽과 디스크 용량을 낭비하게 됩니다.
 * 2. 브라우저의 HTML5 Canvas API를 활용해 클라이언트 측에서 이미지를 즉시 비동기 분석하고,
 *    가로 최대 폭을 1200px(에디토리얼 인쇄 표준)로 자동 다운스케일링 & 88% 고품질 JPEG로 압축합니다.
 * 3. 툴바 버튼을 통한 파일 선택은 물론, [드래그 & 드롭] 및 클립보드 [Ctrl + V] 단축키로도
 *    즉각 이미지 업로드가 가능하도록 사용자 경험(UX)을 극대화합니다.
 */

// 에디토리얼 조판에 최적화된 최대 이미지 가로 너비 (단위: px)
const MAX_IMAGE_WIDTH = 1200;

/**
 * 이미지 파일을 브라우저에서 자동 리사이징/압축 후 백엔드로 업로드하는 함수
 * @param {File|Blob} file - 사용자가 선택하거나 붙여넣은 이미지 파일 객체
 * @param {Function} onUploaded - 업로드 완료 시 (imageUrl, originalName)을 전달받는 콜백
 */
function resizeAndUploadImage(file, onUploaded) {
    // 1. 유효한 이미지 파일인지 MIME 타입 검증
    if (!file || !file.type.startsWith("image/")) {
        showToast("PLEASE SELECT A VALID IMAGE FILE.");
        return;
    }

    showToast("OPTIMIZING & UPLOADING IMAGE...");

    // 2. FileReader로 이미지 파일을 Data URL로 읽어들임
    const reader = new FileReader();
    reader.onload = function (e) {
        const img = new Image();
        img.onload = function () {
            let width = img.width;
            let height = img.height;

            // 3. 비율 유지(Aspect Ratio)를 계산하며 1200px 초과 시 다운스케일링
            if (width > MAX_IMAGE_WIDTH) {
                height = Math.round((height * MAX_IMAGE_WIDTH) / width);
                width = MAX_IMAGE_WIDTH;
            }

            // 4. 가상 캔버스를 생성해 축소된 크기로 그림
            const canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;

            const ctx = canvas.getContext("2d");
            ctx.drawImage(img, 0, 0, width, height);

            // 5. 캔버스 내용을 JPEG(품질 0.88) Blob 바이너리로 변환
            canvas.toBlob(function (blob) {
                if (!blob) {
                    showToast("IMAGE COMPRESSION FAILED.");
                    return;
                }

                // 6. multipart/form-data 전송용 FormData 객체 구성
                const formData = new FormData();
                const baseName = file.name ? file.name.replace(/\.[^/.]+$/, "") : "보도사진";
                const uploadFileName = baseName + ".jpg";
                formData.append("file", blob, uploadFileName);

                const accessToken = localStorage.getItem("access_token") || getCookie("access_token");

                // 7. 백엔드 이미지 업로드 API(/api/images/upload)로 전송
                fetch("/api/images/upload", {
                    method: "POST",
                    headers: {
                        Authorization: accessToken ? `Bearer ${accessToken}` : ""
                    },
                    body: formData
                })
                    .then(res => {
                        if (!res.ok) throw new Error("Upload failed");
                        return res.json();
                    })
                    .then(data => {
                        showToast("IMAGE UPLOAD COMPLETE.");
                        const cleanOriginalName = (data.originalName || file.name || "보도사진").replace(/\.[^/.]+$/, "");
                        // 성공 콜백 호출 (에디터 본문에 마크다운 태그 삽입)
                        if (onUploaded) onUploaded(data.url, cleanOriginalName);
                    })
                    .catch(err => {
                        console.error(err);
                        showToast("FAILED TO UPLOAD IMAGE.");
                    });

            }, "image/jpeg", 0.88);
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

/**
 * 에디터 입력 요소들에 드래그&드롭 및 클립보드 붙여넣기(Ctrl+V) 이벤트를 일괄 바인딩하는 함수
 * @param {HTMLElement[]} elements - 이벤트를 연결할 텍스트에어리어 및 캔버스 요소들의 배열
 * @param {Function} onUploaded - 업로드 성공 시 호출할 콜백
 */
function bindImageDragAndDrop(elements, onUploaded) {
    elements.forEach(el => {
        if (!el) return;

        // 드래그 요소가 위에 올라왔을 때 (점선 보더 강조 스타일)
        el.addEventListener("dragover", (e) => {
            e.preventDefault();
            el.classList.add("dragover");
        });

        // 드래그가 벗어났을 때 스타일 복구
        el.addEventListener("dragleave", () => {
            el.classList.remove("dragover");
        });

        // 이미지를 드롭(놓았을 때) 업로드 파이프라인 가동
        el.addEventListener("drop", (e) => {
            e.preventDefault();
            el.classList.remove("dragover");
            if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]) {
                const file = e.dataTransfer.files[0];
                if (file.type.startsWith("image/")) {
                    resizeAndUploadImage(file, onUploaded);
                }
            }
        });

        // 클립보드 캡처 후 붙여넣기(Ctrl + V) 처리
        el.addEventListener("paste", (e) => {
            const items = (e.clipboardData || e.originalEvent?.clipboardData)?.items;
            if (!items) return;
            for (let index in items) {
                const item = items[index];
                if (item.kind === 'file' && item.type.startsWith('image/')) {
                    const blob = item.getAsFile();
                    resizeAndUploadImage(blob, onUploaded);
                    e.preventDefault(); // 기본 붙여넣기 동작 중지
                    break;
                }
            }
        });
    });
}
