package com.example.myblog.dto;

import com.example.myblog.domain.Article;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class ArticleListViewResponseDto {
    private Long id;
    private String title;
    private String content;
    private long views;
    private LocalDateTime createdAt;

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ALT_PATTERN = Pattern.compile("alt=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public ArticleListViewResponseDto(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.views = article.getViews();
        this.createdAt = article.getCreatedAt();
    }

    /**
     * 본문(마크다운 또는 HTML)에서 첫 번째 이미지 URL 추출
     */
    public String getFirstImageUrl() {
        if (content == null) return null;

        // 1. 마크다운 형식 검사 (![alt](url))
        Matcher mdMatcher = MD_IMAGE_PATTERN.matcher(content);
        if (mdMatcher.find()) {
            return mdMatcher.group(2);
        }

        // 2. HTML 태그 형식 검사 (<img src="...">)
        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(content);
        if (htmlMatcher.find()) {
            return htmlMatcher.group(1);
        }

        return null;
    }

    /**
     * 본문(마크다운 또는 HTML)에서 첫 번째 이미지 alt 텍스트 추출
     */
    public String getFirstImageAlt() {
        if (content == null) return "보도 사진";

        // 1. 마크다운 alt 검사
        Matcher mdMatcher = MD_IMAGE_PATTERN.matcher(content);
        if (mdMatcher.find()) {
            String alt = mdMatcher.group(1).trim();
            return alt.isEmpty() ? "보도 사진" : alt;
        }

        // 2. HTML img 태그의 alt 검사
        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(content);
        if (htmlMatcher.find()) {
            String imgTag = htmlMatcher.group(0);
            Matcher altMatcher = HTML_ALT_PATTERN.matcher(imgTag);
            if (altMatcher.find()) {
                String alt = altMatcher.group(1).trim();
                return alt.isEmpty() ? "보도 사진" : alt;
            }
        }

        return "보도 사진";
    }

    /**
     * 신문 지면용 사진 캡션 포맷팅 ([이미지] 파일명 형태, 확장자 제거 및 한글 보존)
     */
    public String getFirstImageCaption() {
        if (content == null) return "[이미지] 보도사진";

        String rawName = null;

        // 1. 마크다운 alt 검사
        Matcher mdMatcher = MD_IMAGE_PATTERN.matcher(content);
        if (mdMatcher.find()) {
            rawName = mdMatcher.group(1).trim();
            if (rawName.isEmpty()) {
                rawName = extractFilenameFromUrl(mdMatcher.group(2));
            }
        }

        // 2. HTML img alt / src 검사
        if (rawName == null || rawName.isEmpty()) {
            Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(content);
            if (htmlMatcher.find()) {
                String imgTag = htmlMatcher.group(0);
                Matcher altMatcher = HTML_ALT_PATTERN.matcher(imgTag);
                if (altMatcher.find()) {
                    rawName = altMatcher.group(1).trim();
                }
                if (rawName == null || rawName.isEmpty()) {
                    rawName = extractFilenameFromUrl(htmlMatcher.group(1));
                }
            }
        }

        if (rawName == null || rawName.isEmpty()) {
            return "[이미지] 보도사진";
        }

        // URL 인코딩 되어 있는 경우 디코딩 (한글 복원)
        try {
            if (rawName.contains("%")) {
                rawName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        // 확장자 (.jpg, .jpeg, .png, .gif, .webp, .svg, .bmp 등) 제거
        String cleanName = rawName.replaceAll("(?i)\\.(jpg|jpeg|png|webp|gif|svg|bmp)$", "").trim();

        // 기존에 [이미지] 가 이미 붙어있는 경우 중복 제거
        if (cleanName.startsWith("[이미지]")) {
            cleanName = cleanName.substring(5).trim();
        }

        if (cleanName.isEmpty()) {
            return "[이미지] 보도사진";
        }

        return "[이미지] " + cleanName;
    }

    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return url;
    }

    /**
     * 본문에 이미지가 포함되어 있는지 여부
     */
    public boolean isHasImage() {
        return getFirstImageUrl() != null;
    }

    /**
     * 신문 지면용 순수 텍스트 요약문 추출 (마크다운, HTML, 이미지 파일명/캡션 잔재 정제 및 길이 제한)
     */
    public String getCleanExcerpt(int maxLength) {
        if (content == null) return "";

        String clean = extractCleanText(content);

        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength).trim() + "…";
    }

    /**
     * 텍스트가 잘려 점프라인(<[더보기] 란에서 이어짐>)이 필요한지 여부
     */
    public boolean isNeedsJumpLine(int maxLength) {
        if (content == null) return false;
        String clean = extractCleanText(content);
        return clean.length() > maxLength;
    }

    /**
     * 마크다운 태그, HTML 태그, 단독 이미지 파일명/캡션 잔재를 정제한 순수 텍스트 추출
     */
    private String extractCleanText(String raw) {
        if (raw == null) return "";

        // 1. 마크다운 이미지 문법 전체 제거 (![alt](url))
        String clean = raw.replaceAll("!\\[.*?\\]\\(.*?\\)", " ");

        // 2. HTML 태그 전체 제거
        clean = clean.replaceAll("<[^>]*>", " ");

        // 3. 이미지 캡션 잔재(*[이미지] 캡션*, *캡션*, [이미지] 캡션 등) 정제
        clean = clean.replaceAll("(?i)\\*?\\[(이미지|사진|Photo|Picture|Image)\\].*?\\*?", " ");
        clean = clean.replaceAll("(?i)\\[(이미지|사진|Photo|Picture|Image).*?\\]", " ");

        // 4. 마크다운 링크에서 텍스트만 추출 ([text](url) -> text)
        clean = clean.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");

        // 5. 본문에 포함된 단독 이미지 파일명 잔재 제거 (예: 4ad3b567236d7d59e2f31e6633cfcdc7.jpg, sample.png 등)
        clean = clean.replaceAll("(?i)\\b[\\w가-힣._-]+\\.(jpg|jpeg|png|webp|gif|svg|bmp)\\b", " ");

        // 6. 마크다운 서식 기호 제거 (#, *, >, `, _, ~, -)
        clean = clean.replaceAll("[#*>`_~-]", " ");

        // 7. 연속된 공백을 단일 공백으로 치환 후 앞뒤 트림
        return clean.replaceAll("\\s+", " ").trim();
    }
}
