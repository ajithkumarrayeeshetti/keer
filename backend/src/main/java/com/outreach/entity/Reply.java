package com.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "replies")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Reply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    private ReplyClassification classification = ReplyClassification.UNKNOWN;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ReplyClassification { POSITIVE, INTERVIEW, REJECTION, NEUTRAL, UNKNOWN }
}
