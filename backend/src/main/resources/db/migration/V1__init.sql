-- AI Job Outreach Platform — Complete Base Schema
-- V1__init.sql  (Flyway managed)

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resumes (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    filename     VARCHAR(255) NOT NULL,
    file_path    VARCHAR(500) NOT NULL,
    raw_text     LONGTEXT,
    skills       JSON,
    technologies JSON,
    projects     JSON,
    experience   JSON,
    education    JSON,
    parsed_at    TIMESTAMP    NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS jobs (
    id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    company            VARCHAR(255) NOT NULL,
    role               VARCHAR(255) NOT NULL,
    match_score        INT,
    location           VARCHAR(255),
    job_type           VARCHAR(100),
    hr_name            VARCHAR(255),
    hr_email           VARCHAR(255),
    recruiter_name     VARCHAR(255),
    recruiter_linkedin VARCHAR(500),
    company_website    VARCHAR(500),
    company_size       VARCHAR(100),
    tech_stack         TEXT,
    job_description    LONGTEXT,
    why_match          TEXT,
    suggested_subject  VARCHAR(500),
    application_link   VARCHAR(500),
    status             ENUM('PENDING','EMAIL_GENERATED','SENT','REPLIED','REJECTED','SKIPPED') DEFAULT 'PENDING',
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS emails (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    job_id          BIGINT       NOT NULL,
    subject         VARCHAR(500),
    body            LONGTEXT,
    status          ENUM('DRAFT','APPROVED','SENT','FAILED') DEFAULT 'DRAFT',
    sent_at         TIMESTAMP    NULL,
    opened_at       TIMESTAMP    NULL        COMMENT 'Set when tracking pixel fires',
    tracking_token  VARCHAR(36)  UNIQUE      COMMENT 'UUID for open-tracking pixel',
    retry_count     INT          DEFAULT 0   COMMENT 'Send retry count after FAILED',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id)  REFERENCES jobs(id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS applications (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    job_id        BIGINT       NOT NULL,
    email_id      BIGINT,
    company       VARCHAR(255),
    role          VARCHAR(255),
    hr_name       VARCHAR(255),
    hr_email      VARCHAR(255),
    subject       VARCHAR(500),
    email_content LONGTEXT,
    sent_at       TIMESTAMP    NULL,
    status        ENUM('SENT','REPLIED_POSITIVE','INTERVIEW','REJECTED','NO_RESPONSE') DEFAULT 'SENT',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (job_id)   REFERENCES jobs(id)    ON DELETE CASCADE,
    FOREIGN KEY (email_id) REFERENCES emails(id)  ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS followups (
    id              BIGINT  AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT  NOT NULL,
    user_id         BIGINT  NOT NULL,
    sequence_number INT     DEFAULT 1,
    subject         VARCHAR(500),
    body            LONGTEXT,
    scheduled_at    TIMESTAMP NULL,
    sent_at         TIMESTAMP NULL,
    status          ENUM('PENDING','SENT','CANCELLED') DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS replies (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    message_id      VARCHAR(255),
    from_address    VARCHAR(255),
    subject         VARCHAR(500),
    body            LONGTEXT,
    classification  ENUM('POSITIVE','INTERVIEW','REJECTION','NEUTRAL','UNKNOWN') DEFAULT 'UNKNOWN',
    received_at     TIMESTAMP    NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS settings (
    id                        BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id                   BIGINT       UNIQUE NOT NULL,
    gmail_address             VARCHAR(255),
    gmail_app_password        VARCHAR(500) COMMENT 'AES-256-GCM encrypted',
    ai_provider               VARCHAR(50)  DEFAULT 'OLLAMA',
    ai_api_key                VARCHAR(500) COMMENT 'AES-256-GCM encrypted',
    ai_model                  VARCHAR(100),
    ollama_model              VARCHAR(100) DEFAULT 'qwen3',
    ollama_url                VARCHAR(255) DEFAULT 'http://ollama:11434',
    followup_day1             INT          DEFAULT 7,
    followup_day2             INT          DEFAULT 14,
    email_signature           TEXT,
    batch_send_delay_seconds  INT          DEFAULT 45 COMMENT 'Delay between batch sends',
    created_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Performance indexes
CREATE INDEX idx_jobs_user_status   ON jobs(user_id, status);
CREATE INDEX idx_emails_job         ON emails(job_id);
CREATE INDEX idx_emails_user_status ON emails(user_id, status);
CREATE INDEX idx_emails_tracking    ON emails(tracking_token);
CREATE INDEX idx_apps_user          ON applications(user_id);
CREATE INDEX idx_apps_status        ON applications(status);
CREATE INDEX idx_followups_sched    ON followups(scheduled_at, status);
CREATE INDEX idx_followups_user     ON followups(user_id);
CREATE INDEX idx_replies_app        ON replies(application_id);
CREATE INDEX idx_replies_msgid      ON replies(message_id);