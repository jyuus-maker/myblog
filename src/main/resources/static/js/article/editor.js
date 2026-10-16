/**
 * =========================================================================
 * THE JOURNAL — Dual Writing Desk & Editor Controller (editor.js)
 * =========================================================================
 * [역할 및 설계 의도]
 * 1. [신문 편집자 모드 (2단 스플릿)]과 [위지윅 모드], [</> HTML 소스코드 보기]의 3가지 뷰를 제공합니다.
 * 2. 어느 모드에서 글을 쓰다가 다른 모드로 전환하더라도 작성 중인 내용이 1글자도 유실되지 않도록
 *    마크다운 ↔ HTML 무손실 양방향 동기화(Lossless Bidirectional Sync)를 수행합니다.
 * 3. 폼 제출 시 제목과 본문 누락 여부를 인라인(붉은 보더 및 에러 텍스트)으로 즉각 검증하여
 *    사용자 경험(UX)을 대폭 향상합니다.
 * 4. 기사 등록(POST), 수정(PUT), 삭제(DELETE) 비동기 통신을 담당합니다.
 */

/**
 * 위지윅 캔버스의 HTML DOM을 깔끔한 순수 마크다운(Markdown) 문자열로 변환하는 파서
 * @param {string} html - HTML 문자열
 * @returns {string} 변환된 마크다운 문자열
 */
function htmlToMarkdown(html) {
    if (!html) return "";
    const container = document.createElement("div");
    container.innerHTML = html;

    function processNode(node) {
        if (node.nodeType === Node.TEXT_NODE) {
            return node.nodeValue;
        }
        if (node.nodeType !== Node.ELEMENT_NODE) {
            return "";
        }

        const tag = node.tagName.toLowerCase();
        let childContent = "";
        node.childNodes.forEach(child => {
            childContent += processNode(child);
        });

        // HTML 태그별 대응 마크다운 문법 매핑
        switch (tag) {
            case "h1": return `\n\n# ${childContent.trim()}\n\n`;
            case "h2": return `\n\n## ${childContent.trim()}\n\n`;
            case "h3": return `\n\n### ${childContent.trim()}\n\n`;
            case "h4": return `\n\n#### ${childContent.trim()}\n\n`;
            case "h5":
            case "h6": return `\n\n##### ${childContent.trim()}\n\n`;
            case "strong":
            case "b": return childContent.trim() ? `**${childContent.trim()}**` : "";
            case "em":
            case "i": return childContent.trim() ? `*${childContent.trim()}*` : "";
            case "blockquote": {
                const cleaned = childContent.trim().split("\n").map(l => l.trim()).filter(Boolean).join("\n> ");
                return cleaned ? `\n\n> ${cleaned}\n\n` : "";
            }
            case "hr": return `\n\n---\n\n`;
            case "ul":
            case "ol": return `\n\n${childContent.trim()}\n\n`;
            case "li": return `- ${childContent.trim()}\n`;
            case "pre": {
                const codeEl = node.querySelector("code");
                const codeText = codeEl ? codeEl.textContent : node.textContent;
                return `\n\n\`\`\`\n${codeText.trim()}\n\`\`\`\n\n`;
            }
            case "code": {
                if (node.parentNode && node.parentNode.tagName.toLowerCase() === "pre") {
                    return childContent;
                }
                return `\`${childContent}\``;
            }
            case "img": {
                const src = node.getAttribute("src") || "";
                const alt = node.getAttribute("alt") || "보도사진";
                return `\n\n![${alt}](${src})\n\n`;
            }
            case "a": {
                const href = node.getAttribute("href") || "#";
                return `[${childContent.trim()}](${href})`;
            }
            case "p":
            case "div": {
                const trimmed = childContent.trim();
                return trimmed ? `\n\n${trimmed}\n\n` : "";
            }
            case "br": return "\n";
            default: return childContent;
        }
    }

    let markdown = "";
    container.childNodes.forEach(child => {
        markdown += processNode(child);
    });

    return markdown.replace(/\n{3,}/g, "\n\n").trim();
}

/**
 * 텍스트에어리어의 현재 커서 위치에 텍스트를 삽입하는 헬퍼 함수
 */
function insertTextAtCursor(textarea, textToInsert) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    textarea.value = text.substring(0, start) + textToInsert + text.substring(end);
    textarea.focus();
    textarea.setSelectionRange(start + textToInsert.length, start + textToInsert.length);
}

/**
 * 마크다운 코드 블록(```...```) 삽입 헬퍼
 */
function insertCodeBlockMarkdown(textarea) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selected = text.substring(start, end);
    const codeContent = selected || "// 코드를 입력하세요";
    const replacement = `\n\`\`\`\n${codeContent}\n\`\`\`\n`;

    textarea.value = text.substring(0, start) + replacement + text.substring(end);
    textarea.focus();
    textarea.setSelectionRange(start + 5, start + 5 + codeContent.length);
}

/**
 * 마크다운 이미지 태그(![...](...)) 삽입 헬퍼
 */
function insertImageMarkdown(textarea, imageUrl, description = "보도사진") {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const cleanDesc = (description || "보도사진").replace(/[\[\]]/g, "").trim();
    const markdown = `\n\n![${cleanDesc}](${imageUrl})\n\n`;

    textarea.value = text.substring(0, start) + markdown + text.substring(end);
    textarea.focus();
    textarea.setSelectionRange(start + markdown.length, start + markdown.length);
}

/**
 * 마크다운 에디터 툴바 서식 적용 함수 (H2, H3, Bold, Italic, Quote 등)
 */
function applyMarkdownFormatting(textarea, action) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selected = text.substring(start, end);

    let replacement = "";
    let cursorOffset = 0;

    switch (action) {
        case "h2": replacement = `\n## ${selected || "소제목"}\n`; cursorOffset = 4; break;
        case "h3": replacement = `\n### ${selected || "하위제목"}\n`; cursorOffset = 5; break;
        case "bold": replacement = `**${selected || "강조 텍스트"}**`; cursorOffset = 2; break;
        case "italic": replacement = `*${selected || "기울임 텍스트"}*`; cursorOffset = 1; break;
        case "quote": replacement = `\n> ${selected || "인용구 및 발췌문을 입력하세요."}\n`; cursorOffset = 3; break;
        case "hr": replacement = `\n\n---\n\n`; cursorOffset = 7; break;
        case "list": replacement = `\n- ${selected || "항목 내용"}\n`; cursorOffset = 3; break;
    }

    textarea.value = text.substring(0, start) + replacement + text.substring(end);
    textarea.focus();
    
    if (selected.length === 0) {
        textarea.setSelectionRange(start + cursorOffset, start + replacement.length - cursorOffset);
    } else {
        textarea.setSelectionRange(start + replacement.length, start + replacement.length);
    }
}

/**
 * HTML 소스코드 편집기 툴바 태그 삽입 함수
 */
function applySourceTagFormatting(textarea, action) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selected = text.substring(start, end);

    let openTag = "", closeTag = "", defaultText = "";

    switch (action) {
        case "h2": openTag = "<h2>"; closeTag = "</h2>\n"; defaultText = "소제목"; break;
        case "h3": openTag = "<h3>"; closeTag = "</h3>\n"; defaultText = "하위제목"; break;
        case "bold": openTag = "<strong>"; closeTag = "</strong>"; defaultText = "강조 텍스트"; break;
        case "italic": openTag = "<em>"; closeTag = "</em>"; defaultText = "기울임 텍스트"; break;
        case "quote": openTag = "<blockquote><p>"; closeTag = "</p></blockquote>\n"; defaultText = "인용구 및 발췌문"; break;
        case "hr": openTag = "\n<hr>\n"; closeTag = ""; defaultText = ""; break;
        case "list": openTag = "<ul>\n  <li>"; closeTag = "</li>\n</ul>\n"; defaultText = "항목 내용"; break;
    }

    const insert = openTag + (selected || defaultText) + closeTag;
    textarea.value = text.substring(0, start) + insert + text.substring(end);
    textarea.focus();
    textarea.setSelectionRange(start + openTag.length, start + openTag.length + (selected || defaultText).length);
}

/**
 * 위지윅 캔버스 서식 적용 함수 (document.execCommand)
 */
function applyVisualFormatting(action) {
    switch (action) {
        case "h2": document.execCommand("formatBlock", false, "<h2>"); break;
        case "h3": document.execCommand("formatBlock", false, "<h3>"); break;
        case "bold": document.execCommand("bold", false, null); break;
        case "italic": document.execCommand("italic", false, null); break;
        case "quote": document.execCommand("formatBlock", false, "<blockquote>"); break;
        case "hr": document.execCommand("insertHorizontalRule", false, null); break;
        case "list": document.execCommand("insertUnorderedList", false, null); break;
    }
}

/**
 * 기사 작성/수정 폼 유효성 검증 함수 (빈 값 제출 시 붉은 테두리 및 인라인 에러 표시)
 * @param {string} titleVal - 제목 문자열
 * @param {string} contentVal - 본문 문자열
 * @returns {boolean} 유효성 검과 여부 (true: 통과, false: 결함)
 */
function validateArticleForm(titleVal, contentVal) {
    let isValid = true;
    const titleInput = document.querySelector("#title");
    const titleError = document.querySelector("#title-error");
    const contentError = document.querySelector("#content-error");
    const splitInput = document.querySelector("#split-markdown-input");
    const editorCanvas = document.querySelector("#editor-canvas");

    // 이전 에러 상태 초기화
    if (titleInput) titleInput.classList.remove("input-has-error");
    if (titleError) titleError.classList.remove("show");
    if (contentError) contentError.classList.remove("show");
    if (splitInput) splitInput.classList.remove("pane-has-error");
    if (editorCanvas) editorCanvas.classList.remove("pane-has-error");

    // 1. 제목 누락 검증
    if (!titleVal) {
        if (titleInput) {
            titleInput.classList.add("input-has-error");
            titleInput.focus();
        }
        if (titleError) titleError.classList.add("show");
        isValid = false;
    }

    // 2. 본문 누락 검증
    if (!contentVal || contentVal === "<br>") {
        if (contentError) contentError.classList.add("show");
        if (splitInput && splitInput.offsetParent !== null) {
            splitInput.classList.add("pane-has-error");
            if (isValid) splitInput.focus();
        } else if (editorCanvas && editorCanvas.offsetParent !== null) {
            editorCanvas.classList.add("pane-has-error");
            if (isValid) editorCanvas.focus();
        }
        isValid = false;
    }

    return isValid;
}

// -----------------------------------------------------------------------------
// DOM 로드 완료 후 에디터 초기화 및 이벤트 바인딩
// -----------------------------------------------------------------------------
document.addEventListener("DOMContentLoaded", function () {
    // 글 작성 페이지 비로그인 접근 차단
    if (window.location.pathname === "/new-article" && !isLoggedIn()) {
        showToast("SIGN IN REQUIRED FOR AUTHORING");
        setTimeout(() => { location.replace("/login"); }, 800);
        return;
    }

    // 뷰 스위처 버튼 및 작업공간 DOM 참조
    const btnModeSplit = document.querySelector("#btn-mode-split");
    const btnModeVisual = document.querySelector("#btn-mode-visual");
    const btnToolbarSource = document.querySelector("#btn-toolbar-source");

    const deskSplitWorkspace = document.querySelector("#desk-split-workspace");
    const deskVisualWorkspace = document.querySelector("#desk-visual-workspace");
    const deskCodeWorkspace = document.querySelector("#desk-code-workspace");

    const splitMarkdownInput = document.querySelector("#split-markdown-input");
    const splitLivePreview = document.querySelector("#split-live-preview");
    const editorCanvas = document.querySelector("#editor-canvas");
    const editorHtmlSource = document.querySelector("#editor-html-source");

    const rawContentSource = document.querySelector("#raw-content-source");
    const charCounter = document.querySelector("#char-counter");
    const imageFileInput = document.querySelector("#image-file-input");
    const uploadImageBtn = document.querySelector("#upload-image-btn");

    if (splitMarkdownInput && editorCanvas && editorHtmlSource) {
        let currentMode = "split"; // 'split' | 'visual'
        let isCodeView = false;    // HTML 원본 소스코드 편집 상태 여부

        // 수정 페이지 진입 시 기존 저장된 데이터 복원 (HTML 형식인 경우 마크다운으로 자동 변환)
        const initialRaw = rawContentSource ? (rawContentSource.value || rawContentSource.textContent || "").trim() : "";
        let initialMarkdown = initialRaw;

        if (initialRaw.startsWith("<") && (initialRaw.includes("</p>") || initialRaw.includes("</div>") || initialRaw.includes("</h2>") || initialRaw.includes("</blockquote>"))) {
            initialMarkdown = htmlToMarkdown(initialRaw);
        }

        splitMarkdownInput.value = initialMarkdown;

        if (typeof marked !== "undefined") {
            marked.setOptions({ breaks: true, gfm: true });
            const renderedHtml = marked.parse(initialMarkdown || "");
            splitLivePreview.innerHTML = renderedHtml || "<p><em>*오른쪽에 본문 및 첨부한 이미지가 실시간 신문 조판으로 렌더링됩니다.*</em></p>";
            editorCanvas.innerHTML = renderedHtml;
        }

        // 실시간 글자수 및 단어수 집계 함수
        const updateStats = () => {
            let text = "";
            if (isCodeView) {
                text = editorHtmlSource.value;
            } else if (currentMode === "split") {
                text = splitMarkdownInput.value;
            } else {
                text = editorCanvas.innerText || "";
            }
            const words = text.trim() ? text.trim().split(/\s+/).length : 0;
            const chars = text.length;
            if (charCounter) {
                charCounter.textContent = `${words} WORDS / ${chars} CHARACTERS`;
            }
        };

        // 실시간 신문 조판 프리뷰 업데이트 함수
        const updateSplitPreview = () => {
            const val = splitMarkdownInput.value || "";
            updateStats();
            if (typeof marked !== "undefined") {
                splitLivePreview.innerHTML = marked.parse(val || "<p><em>*오른쪽에 본문 및 첨부한 이미지가 실시간 신문 조판으로 렌더링됩니다.*</em></p>");
            }
        };

        splitMarkdownInput.addEventListener("input", updateSplitPreview);
        editorCanvas.addEventListener("input", updateStats);
        editorHtmlSource.addEventListener("input", updateStats);
        updateSplitPreview();

        function formatHtmlCode(html) {
            if (!html) return "";
            return html
                .replace(/></g, ">\n<")
                .replace(/(<br\s*\/?>)+/gi, "<br>\n")
                .trim();
        }

        // 에디터 모드 전환 (2단 스플릿 vs 위지윅)
        function switchEditorMode(mode) {
            if (isCodeView) exitCodeView();

            if (mode === "split") {
                if (currentMode === "visual" && editorCanvas) {
                    splitMarkdownInput.value = htmlToMarkdown(editorCanvas.innerHTML);
                }
                currentMode = "split";
                if (deskSplitWorkspace) deskSplitWorkspace.style.display = "grid";
                if (deskVisualWorkspace) deskVisualWorkspace.style.display = "none";
                if (deskCodeWorkspace) deskCodeWorkspace.style.display = "none";
                if (btnModeSplit) btnModeSplit.classList.add("active");
                if (btnModeVisual) btnModeVisual.classList.remove("active");
                updateSplitPreview();
            } else if (mode === "visual") {
                if (currentMode === "split" && splitMarkdownInput) {
                    editorCanvas.innerHTML = typeof marked !== "undefined" ? marked.parse(splitMarkdownInput.value || "") : splitMarkdownInput.value || "";
                }
                currentMode = "visual";
                if (deskSplitWorkspace) deskSplitWorkspace.style.display = "none";
                if (deskVisualWorkspace) deskVisualWorkspace.style.display = "block";
                if (deskCodeWorkspace) deskCodeWorkspace.style.display = "none";
                if (btnModeSplit) btnModeSplit.classList.remove("active");
                if (btnModeVisual) btnModeVisual.classList.add("active");
                updateStats();
            }
        }

        if (btnModeSplit && btnModeVisual) {
            btnModeSplit.addEventListener("click", () => switchEditorMode("split"));
            btnModeVisual.addEventListener("click", () => switchEditorMode("visual"));
            switchEditorMode("split");
        }

        // </> HTML 소스코드 편집 모드 진입
        function enterCodeView() {
            isCodeView = true;
            let generatedHtml = "";
            if (currentMode === "split") {
                generatedHtml = typeof marked !== "undefined" ? marked.parse(splitMarkdownInput.value || "") : splitMarkdownInput.value;
            } else {
                generatedHtml = editorCanvas.innerHTML;
            }

            editorHtmlSource.value = formatHtmlCode(generatedHtml);

            if (deskSplitWorkspace) deskSplitWorkspace.style.display = "none";
            if (deskVisualWorkspace) deskVisualWorkspace.style.display = "none";
            if (deskCodeWorkspace) deskCodeWorkspace.style.display = "block";
            if (btnToolbarSource) btnToolbarSource.classList.add("active");

            editorHtmlSource.focus();
            updateStats();
            showToast("</> 단일 캔버스 소스코드 편집 모드 활성화");
        }

        // </> HTML 소스코드 편집 모드 퇴출
        function exitCodeView() {
            isCodeView = false;
            const rawSource = editorHtmlSource.value;

            if (currentMode === "split") {
                splitMarkdownInput.value = htmlToMarkdown(rawSource);
                if (typeof marked !== "undefined") {
                    splitLivePreview.innerHTML = marked.parse(splitMarkdownInput.value || "");
                }
                if (deskSplitWorkspace) deskSplitWorkspace.style.display = "grid";
                if (deskVisualWorkspace) deskVisualWorkspace.style.display = "none";
            } else {
                editorCanvas.innerHTML = rawSource;
                if (deskSplitWorkspace) deskSplitWorkspace.style.display = "none";
                if (deskVisualWorkspace) deskVisualWorkspace.style.display = "block";
            }

            if (deskCodeWorkspace) deskCodeWorkspace.style.display = "none";
            if (btnToolbarSource) btnToolbarSource.classList.remove("active");

            updateStats();
            showToast("에디터 뷰 복귀 및 코드 변경 동기화 완료");
        }

        function toggleCodeView() {
            if (!isCodeView) enterCodeView();
            else exitCodeView();
        }

        // 툴바 버튼 이벤트 연결
        document.querySelectorAll(".toolbar-btn[data-action]").forEach(btn => {
            btn.addEventListener("click", () => {
                const action = btn.dataset.action;
                if (action === "source-view") {
                    toggleCodeView();
                    return;
                }
                if (action === "code-insert") {
                    if (isCodeView) {
                        insertTextAtCursor(editorHtmlSource, "\n<pre><code>// 코드를 입력하세요\n</code></pre>\n");
                    } else if (currentMode === "split") {
                        insertCodeBlockMarkdown(splitMarkdownInput);
                        updateSplitPreview();
                    } else {
                        editorCanvas.focus();
                        document.execCommand("insertHTML", false, `<pre><code>// 코드를 입력하세요\n</code></pre><p><br></p>`);
                    }
                    updateStats();
                    showToast("💻 코드 블록이 삽입되었습니다.");
                    return;
                }

                if (isCodeView) {
                    applySourceTagFormatting(editorHtmlSource, action);
                    updateStats();
                } else if (currentMode === "split") {
                    applyMarkdownFormatting(splitMarkdownInput, action);
                    updateSplitPreview();
                } else {
                    editorCanvas.focus();
                    applyVisualFormatting(action);
                    updateStats();
                }
            });
        });

        // 이미지 업로드 콜백 및 드래그앤드롭/붙여넣기 연결
        const handleImageUploaded = (url, originalName) => {
            const cleanDesc = (originalName || "보도사진").replace(/[\[\]]/g, "").trim();
            if (isCodeView) {
                insertTextAtCursor(editorHtmlSource, `\n<div><img src="${url}" alt="${cleanDesc}"></div>\n<p><em>[이미지] ${cleanDesc}</em></p>\n`);
            } else if (currentMode === "split") {
                insertImageMarkdown(splitMarkdownInput, url, cleanDesc);
                updateSplitPreview();
            } else {
                editorCanvas.focus();
                document.execCommand("insertHTML", false, `<div><img src="${url}" alt="${cleanDesc}"></div><p><em>[이미지] ${cleanDesc}</em></p><p><br></p>`);
            }
            updateStats();
        };

        if (uploadImageBtn && imageFileInput) {
            uploadImageBtn.addEventListener("click", () => imageFileInput.click());
            imageFileInput.addEventListener("change", function () {
                if (this.files && this.files[0]) {
                    resizeAndUploadImage(this.files[0], handleImageUploaded);
                    this.value = "";
                }
            });
        }

        bindImageDragAndDrop([splitMarkdownInput, editorCanvas, editorHtmlSource], handleImageUploaded);
    }

    // -------------------------------------------------------------------------
    // 기사 등록(POST) / 수정(PUT) / 삭제(DELETE) 비동기 처리
    // -------------------------------------------------------------------------
    const createButton = document.querySelector("#create-btn");
    if (createButton) {
        createButton.addEventListener("click", function () {
            const titleInput = document.querySelector("#title");
            const titleVal = titleInput ? titleInput.value.trim() : "";
            const splitMarkdownInput = document.querySelector("#split-markdown-input");
            const editorCanvas = document.querySelector("#editor-canvas");
            const editorHtmlSource = document.querySelector("#editor-html-source");
            const deskCodeWorkspace = document.querySelector("#desk-code-workspace");
            const deskVisualWorkspace = document.querySelector("#desk-visual-workspace");

            let contentVal = "";
            if (deskCodeWorkspace && deskCodeWorkspace.style.display !== "none") {
                contentVal = editorHtmlSource.value.trim();
            } else if (deskVisualWorkspace && deskVisualWorkspace.style.display !== "none") {
                contentVal = htmlToMarkdown(editorCanvas.innerHTML).trim();
            } else {
                contentVal = splitMarkdownInput ? splitMarkdownInput.value.trim() : "";
            }

            // 인라인 유효성 검증
            if (!validateArticleForm(titleVal, contentVal)) {
                showToast("PLEASE FILL IN ALL REQUIRED EDITORIAL FIELDS.");
                return;
            }

            const originalText = createButton.textContent;
            createButton.textContent = "PUBLISHING...";
            createButton.disabled = true;

            const body = JSON.stringify({ title: titleVal, content: contentVal });

            // 공통 httpRequest(POST /api/articles) 호출
            httpRequest("POST", "/api/articles", body,
                () => {
                    showToast("PIECE SUCCESSFULLY PUBLISHED.");
                    setTimeout(() => { location.replace("/articles"); }, 600);
                },
                () => {
                    showToast("FAILED TO PUBLISH PIECE. VERIFY INPUT.");
                    createButton.textContent = originalText;
                    createButton.disabled = false;
                }
            );
        });
    }

    const modifyButton = document.querySelector("#modify-btn");
    if (modifyButton) {
        modifyButton.addEventListener("click", function () {
            let id = document.querySelector("#article-id")?.value;
            if (!id) {
                const params = new URLSearchParams(location.search);
                id = params.get("id");
            }

            const titleInput = document.querySelector("#title");
            const titleVal = titleInput ? titleInput.value.trim() : "";
            const splitMarkdownInput = document.querySelector("#split-markdown-input");
            const editorCanvas = document.querySelector("#editor-canvas");
            const editorHtmlSource = document.querySelector("#editor-html-source");
            const deskCodeWorkspace = document.querySelector("#desk-code-workspace");
            const deskVisualWorkspace = document.querySelector("#desk-visual-workspace");

            let contentVal = "";
            if (deskCodeWorkspace && deskCodeWorkspace.style.display !== "none") {
                contentVal = editorHtmlSource.value.trim();
            } else if (deskVisualWorkspace && deskVisualWorkspace.style.display !== "none") {
                contentVal = htmlToMarkdown(editorCanvas.innerHTML).trim();
            } else {
                contentVal = splitMarkdownInput ? splitMarkdownInput.value.trim() : "";
            }

            // 인라인 유효성 검증
            if (!validateArticleForm(titleVal, contentVal)) {
                showToast("PLEASE FILL IN ALL REQUIRED EDITORIAL FIELDS.");
                return;
            }

            const originalText = modifyButton.textContent;
            modifyButton.textContent = "SAVING...";
            modifyButton.disabled = true;

            const body = JSON.stringify({ title: titleVal, content: contentVal });

            // 공통 httpRequest(PUT /api/articles/{id}) 호출
            httpRequest("PUT", `/api/articles/${id}`, body,
                () => {
                    showToast("CHANGES SUCCESSFULLY SAVED.");
                    setTimeout(() => { location.replace(`/articles/${id}`); }, 600);
                },
                () => {
                    showToast("FAILED TO SAVE CHANGES.");
                    modifyButton.textContent = originalText;
                    modifyButton.disabled = false;
                }
            );
        });
    }

    const deleteButton = document.querySelector("#delete-btn");
    if (deleteButton) {
        deleteButton.addEventListener("click", function () {
            const id = document.querySelector("#article-id")?.value;

            // 브라우저 confirm 대신 커스텀 에디토리얼 모달 호출
            showConfirm(
                `Permanently delete Issue #${id} from The Journal archive?`,
                "PERMANENT REMOVAL",
                () => {
                    deleteButton.textContent = "DELETING...";
                    deleteButton.disabled = true;

                    // 공통 httpRequest(DELETE /api/articles/{id}) 호출
                    httpRequest("DELETE", `/api/articles/${id}`, null,
                        () => {
                            showToast("ENTRY REMOVED FROM ARCHIVE.");
                            setTimeout(() => { location.replace("/articles"); }, 600);
                        },
                        () => {
                            showToast("FAILED TO REMOVE ENTRY.");
                            deleteButton.textContent = "DELETE";
                            deleteButton.disabled = false;
                        }
                    );
                }
            );
        });
    }
});
