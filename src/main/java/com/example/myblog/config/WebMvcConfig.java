package com.example.myblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 웹 MVC, 정적 리소스 핸들러 및 CORS 설정 클래스
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 디렉토리가 없으면 자동 생성
        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // /uploads/** URL 경로를 실제 파일 저장소 디렉토리(URI)로 매핑
        String location = dir.toURI().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080", "http://localhost:8090")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Request-ID")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
