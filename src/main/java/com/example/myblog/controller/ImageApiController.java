package com.example.myblog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 에디토리얼 이미지 업로드 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageApiController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/api/images/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드할 이미지 파일이 비어있습니다."));
        }

        // 원본 파일명 (한글 파일명 유니코드/URL 디코딩 처리)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            try {
                if (originalFilename.contains("%")) {
                    originalFilename = URLDecoder.decode(originalFilename, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }

        // 이미지 파일 확장자 검증
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!extension.matches("\\.(jpg|jpeg|png|webp|gif|svg)$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "지원하지 않는 이미지 형식입니다. (jpg, png, webp, gif 지원)"));
        }

        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 고유한 파일명 생성 (UUID)
            String savedFilename = UUID.randomUUID() + extension;
            Path targetPath = dirPath.resolve(savedFilename);

            Files.copy(file.getInputStream(), targetPath);
            log.info("[ImageApiController] 이미지 업로드 성공: {}", targetPath);

            String fileUrl = "/uploads/" + savedFilename;
            return ResponseEntity.ok(Map.of(
                    "url", fileUrl,
                    "filename", savedFilename,
                    "originalName", originalFilename != null ? originalFilename : savedFilename
            ));

        } catch (IOException e) {
            log.error("[ImageApiController] 파일 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "이미지 업로드 중 서버 오류가 발생했습니다."));
        }
    }
}
