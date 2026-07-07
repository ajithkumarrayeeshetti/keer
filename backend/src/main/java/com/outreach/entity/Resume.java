package com.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Resume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(columnDefinition = "JSON")
    private String skills;

    @Column(columnDefinition = "JSON")
    private String technologies;

    @Column(columnDefinition = "JSON")
    private String projects;

    @Column(columnDefinition = "JSON")
    private String experience;

    @Column(columnDefinition = "JSON")
    private String education;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
