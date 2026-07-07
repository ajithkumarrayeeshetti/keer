# AI Job Outreach Platform — Architecture & Deliverables

## 1. System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              Next.js 15 Frontend (port 3000)                 │
│   Dashboard | Upload | Email Preview | Settings              │
└───────────────────────┬──────────────────────────────────────┘
                        │ REST API + JWT
┌───────────────────────▼──────────────────────────────────────┐
│             Spring Boot 3 Backend (port 8080)                │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │Resume API│ │ Jobs API │ │Email API │ │   Auth API   │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │AI Engine │ │Tracking  │ │Follow-up │ │Inbox Monitor │   │
│  │(Ollama)  │ │Engine    │ │Scheduler │ │(IMAP)        │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└─────────┬──────────────────────────────────┬─────────────────┘
          │ JPA/Hibernate                    │ HTTP
┌─────────▼────────────┐         ┌───────────▼──────────────────┐
│    MySQL (port 3306)  │         │   Ollama (port 11434)         │
│  users | resumes      │         │   Qwen3 | Gemma3 | DeepSeek  │
│  jobs | emails        │         └──────────────────────────────┘
│  applications         │
│  followups | replies  │
│  settings             │
└───────────────────────┘
          │ Gmail SMTP/IMAP
┌─────────▼──────────────────────┐
│     Email Infrastructure       │
│  SMTP (send) | IMAP (receive)  │
└────────────────────────────────┘
```

## 2. Folder Structure

```
outreach-platform/
├── backend/
│   ├── src/main/java/com/outreach/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   ├── MailConfig.java
│   │   │   └── OllamaConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── ResumeController.java
│   │   │   ├── JobController.java
│   │   │   ├── EmailController.java
│   │   │   ├── DashboardController.java
│   │   │   └── SettingsController.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── EmailApprovalRequest.java
│   │   │   │   └── EmailEditRequest.java
│   │   │   └── response/
│   │   │       ├── AuthResponse.java
│   │   │       ├── ResumeProfileResponse.java
│   │   │       ├── JobResponse.java
│   │   │       ├── EmailPreviewResponse.java
│   │   │       ├── DashboardStatsResponse.java
│   │   │       └── ApiResponse.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Resume.java
│   │   │   ├── Job.java
│   │   │   ├── Email.java
│   │   │   ├── Application.java
│   │   │   ├── FollowUp.java
│   │   │   ├── Reply.java
│   │   │   └── Settings.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── EmailSendException.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ResumeRepository.java
│   │   │   ├── JobRepository.java
│   │   │   ├── EmailRepository.java
│   │   │   ├── ApplicationRepository.java
│   │   │   ├── FollowUpRepository.java
│   │   │   ├── ReplyRepository.java
│   │   │   └── SettingsRepository.java
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── service/
│   │   │   ├── ResumeService.java
│   │   │   ├── JobService.java
│   │   │   ├── EmailGenerationService.java
│   │   │   ├── EmailSendingService.java
│   │   │   ├── TrackingService.java
│   │   │   ├── InboxMonitorService.java
│   │   │   ├── FollowUpService.java
│   │   │   ├── DashboardService.java
│   │   │   ├── OllamaService.java
│   │   │   └── impl/
│   │   │       └── (implementations)
│   │   ├── scheduler/
│   │   │   ├── FollowUpScheduler.java
│   │   │   └── InboxPollScheduler.java
│   │   └── util/
│   │       ├── CsvParser.java
│   │       ├── PdfParser.java
│   │       └── PromptBuilder.java
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__init.sql
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx
│   │   │   ├── dashboard/page.tsx
│   │   │   ├── upload/page.tsx
│   │   │   ├── emails/page.tsx
│   │   │   └── settings/page.tsx
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   └── Header.tsx
│   │   │   ├── dashboard/
│   │   │   │   ├── StatsCard.tsx
│   │   │   │   └── ActivityFeed.tsx
│   │   │   └── email/
│   │   │       ├── EmailPreview.tsx
│   │   │       └── EmailEditor.tsx
│   │   ├── lib/
│   │   │   ├── api.ts
│   │   │   └── auth.ts
│   │   ├── types/
│   │   │   └── index.ts
│   │   └── hooks/
│   │       └── useJobs.ts
│   ├── package.json
│   ├── tailwind.config.ts
│   └── next.config.ts
├── docker-compose.yml
├── docker-compose.prod.yml
└── README.md
```

## 3. Database Schema

```sql
-- V1__init.sql

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE resumes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    raw_text LONGTEXT,
    skills JSON,
    technologies JSON,
    projects JSON,
    experience JSON,
    education JSON,
    parsed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    match_score INT,
    location VARCHAR(255),
    job_type VARCHAR(100),
    hr_name VARCHAR(255),
    hr_email VARCHAR(255),
    recruiter_name VARCHAR(255),
    recruiter_linkedin VARCHAR(500),
    company_website VARCHAR(500),
    company_size VARCHAR(100),
    tech_stack TEXT,
    job_description LONGTEXT,
    why_match TEXT,
    suggested_subject VARCHAR(500),
    application_link VARCHAR(500),
    status ENUM('PENDING','EMAIL_GENERATED','SENT','REPLIED','REJECTED','SKIPPED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    subject VARCHAR(500),
    body LONGTEXT,
    status ENUM('DRAFT','APPROVED','SENT','FAILED') DEFAULT 'DRAFT',
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    email_id BIGINT,
    company VARCHAR(255),
    role VARCHAR(255),
    hr_name VARCHAR(255),
    hr_email VARCHAR(255),
    subject VARCHAR(500),
    email_content LONGTEXT,
    sent_at TIMESTAMP,
    status ENUM('SENT','REPLIED_POSITIVE','INTERVIEW','REJECTED','NO_RESPONSE') DEFAULT 'SENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (email_id) REFERENCES emails(id)
);

CREATE TABLE followups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sequence_number INT DEFAULT 1,
    subject VARCHAR(500),
    body LONGTEXT,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    status ENUM('PENDING','SENT','CANCELLED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message_id VARCHAR(255),
    from_address VARCHAR(255),
    subject VARCHAR(500),
    body LONGTEXT,
    classification ENUM('POSITIVE','INTERVIEW','REJECTION','NEUTRAL','UNKNOWN') DEFAULT 'UNKNOWN',
    received_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    gmail_address VARCHAR(255),
    gmail_app_password VARCHAR(500),
    ollama_model VARCHAR(100) DEFAULT 'qwen3',
    ollama_url VARCHAR(255) DEFAULT 'http://ollama:11434',
    followup_day1 INT DEFAULT 7,
    followup_day2 INT DEFAULT 14,
    email_signature TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_jobs_user_id ON jobs(user_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_emails_job_id ON emails(job_id);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_followups_scheduled ON followups(scheduled_at, status);
CREATE INDEX idx_replies_application ON replies(application_id);
```

## 4. API Endpoints

```
AUTH
  POST /api/auth/register
  POST /api/auth/login
  POST /api/auth/refresh

RESUME
  POST   /api/resume/upload          multipart/form-data
  GET    /api/resume                 get parsed profile
  DELETE /api/resume/{id}

JOBS
  POST   /api/jobs/upload            multipart/form-data (CSV)
  GET    /api/jobs                   list all jobs
  GET    /api/jobs/{id}
  PATCH  /api/jobs/{id}/status
  DELETE /api/jobs/{id}

EMAIL GENERATION
  POST   /api/emails/generate/{jobId}  generate AI email for one job
  POST   /api/emails/generate/all      generate for all pending jobs
  GET    /api/emails/{jobId}           get preview
  PUT    /api/emails/{emailId}         edit email content
  PATCH  /api/emails/{emailId}/approve
  PATCH  /api/emails/{emailId}/skip

EMAIL SENDING
  POST   /api/emails/{emailId}/send
  POST   /api/emails/send/batch        send all approved emails

APPLICATIONS
  GET    /api/applications             list all with status
  GET    /api/applications/{id}
  PATCH  /api/applications/{id}/status manual override

FOLLOW-UPS
  GET    /api/followups/pending
  POST   /api/followups/{id}/send      manual send
  DELETE /api/followups/{id}/cancel

DASHBOARD
  GET    /api/dashboard/stats

SETTINGS
  GET    /api/settings
  PUT    /api/settings
  POST   /api/settings/test-connection  test Gmail SMTP
```

## 5. User Flow Summary

```
Upload Resume PDF + jobs.csv
         ↓
Parse resume → extract profile (skills, projects, tech, experience)
Parse CSV    → import job rows
         ↓
For each job: AI generates personalized email (150 words max)
         ↓
User Preview: Approve / Edit / Skip
         ↓
Approved emails → send via Gmail SMTP with resume attached
         ↓
Spring Scheduler polls inbox every 15 min (IMAP)
         ↓
AI classifies replies: positive / interview / rejection / neutral
         ↓
Follow-up scheduler: Day 7, Day 14 (cancels on any reply)
         ↓
Dashboard: live stats across entire pipeline
```
