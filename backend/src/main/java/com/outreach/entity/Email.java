package com.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Email {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    private EmailStatus status = EmailStatus.DRAFT;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** UUID token embedded in tracking pixel URL */
    @Column(name = "tracking_token", unique = true, length = 36)
    private String trackingToken;

    /** How many times delivery has been attempted after a FAILED state */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (trackingToken == null) trackingToken = UUID.randomUUID().toString();
    }

    public enum EmailStatus { DRAFT, APPROVED, SENT, FAILED }
}
