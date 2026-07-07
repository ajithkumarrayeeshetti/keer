package com.outreach.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    @Column(name = "match_score")
    private Integer matchScore;

    private String location;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "hr_name")
    private String hrName;

    @Column(name = "hr_email")
    private String hrEmail;

    @Column(name = "recruiter_name")
    private String recruiterName;

    @Column(name = "recruiter_linkedin", length = 500)
    private String recruiterLinkedin;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "job_description", columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Column(name = "why_match", columnDefinition = "TEXT")
    private String whyMatch;

    @Column(name = "suggested_subject", length = 500)
    private String suggestedSubject;

    @Column(name = "application_link", length = 500)
    private String applicationLink;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum JobStatus { PENDING, EMAIL_GENERATED, SENT, REPLIED, REJECTED, SKIPPED }
}
