package com.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Settings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Gmail
    @Column(name = "gmail_address")
    private String gmailAddress;

    /**
     * Stored AES-256 encrypted (base64).
     * The CredentialEncryptionService handles en/decryption transparently.
     */
    @Column(name = "gmail_app_password", length = 500)
    private String gmailAppPassword;

    // AI Provider selection: OLLAMA | GEMINI | GROQ | TOGETHER | OPENAI
    @Column(name = "ai_provider")
    private String aiProvider = "OLLAMA";

    /** Stored AES-256 encrypted (base64). */
    @Column(name = "ai_api_key", length = 500)
    private String aiApiKey;

    @Column(name = "ai_model")
    private String aiModel;

    // Ollama-specific
    @Column(name = "ollama_url")
    private String ollamaUrl = "http://ollama:11434";

    @Column(name = "ollama_model")
    private String ollamaModel = "qwen3";

    // Follow-up schedule
    @Column(name = "followup_day1")
    private Integer followupDay1 = 7;

    @Column(name = "followup_day2")
    private Integer followupDay2 = 14;

    @Column(name = "email_signature", columnDefinition = "TEXT")
    private String emailSignature;

    /**
     * Seconds to wait between emails in a batch send (default 45s).
     * Prevents Gmail from flagging bulk sends.
     */
    @Column(name = "batch_send_delay_seconds")
    private Integer batchSendDelaySeconds = 45;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
