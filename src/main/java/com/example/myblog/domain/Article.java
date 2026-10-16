package com.example.myblog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 블로그 게시글 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id; // 게시글 식별자 (기본키)

    @Column(name = "title", nullable = false)
    private String title; // 글 제목

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 글 본문

    @Column(name = "views", nullable = false)
    private long views = 0L; // 조회수 (기본값 0)

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt; // 생성 시간 (자동 주입)

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt; // 수정 시간 (자동 주입)

    // 빌더 패턴을 통한 게시글 생성
    @Builder
    public Article(String title, String content, Long views) {
        this.title = title;
        this.content = content;
        this.views = (views != null) ? views : 0L;
    }

    // 게시글 내용 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 조회수 1 증가 메서드
    public void increaseViews() {
        this.views++;
    }
} //end Article
