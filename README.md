# AI Job Outreach Platform

AI-powered cold email outreach system that generates personalized emails, sends them via Gmail, tracks replies, and automates follow-ups using local Ollama models.

---

## Quick Start (Docker)

```bash
# 1. Clone and enter project
cd outreach-platform

# 2. Pull an Ollama model first (needed for AI generation)
docker compose up ollama -d
docker exec outreach-ollama ollama pull qwen3

# 3. Start everything
docker compose up -d

# 4. Open the app
open http://localhost:3000
```

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Docker + Compose | 24+ | Container orchestration |
| Java 21 | JDK 21 | Backend (if running locally) |
| Node.js | 20+ | Frontend (if running locally) |
| Ollama | Latest | Local AI model runner |

---

## Gmail Setup (Required for sending)

1. Enable 2-Factor Authentication on your Google account
2. Go to [Google App Passwords](https://myaccount.google.com/apppasswords)
3. Create an App Password for "Mail"
4. Copy the 16-character password
5. In the app → **Settings** → paste your Gmail address and App Password

---

## Local Development

### Backend

```bash
cd backend

# Set environment variables
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=outreach_user
export DB_PASSWORD=outreach_pass
export OLLAMA_URL=http://localhost:11434
export JWT_SECRET=your-256-bit-secret-key-here

# Start MySQL and Ollama separately first
docker compose up mysql ollama -d

# Run Spring Boot
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install

# Create .env.local
echo "NEXT_PUBLIC_API_URL=http://localhost:8080" > .env.local

npm run dev
# Opens at http://localhost:3000
```

### Ollama (AI Engine)

```bash
# Install Ollama: https://ollama.ai
ollama serve

# Pull your preferred model
ollama pull qwen3      # Recommended - fast and capable
ollama pull gemma3     # Alternative
ollama pull deepseek-r1 # Best quality, slower
```

---

## User Flow

```
1. Register / Sign In
2. Settings → Enter Gmail App Password + choose Ollama model
3. Upload → Drop your resume.pdf
4. Upload → Drop your jobs.csv
5. Emails → Click "Generate All" and wait for AI
6. Review each email → Approve / Edit / Skip
7. Send approved emails (resume auto-attached)
8. Dashboard → Monitor replies, interviews, follow-ups
```

---

## jobs.csv Format

Your CSV must include these columns (export from Claude or any job tracker):

```
Company, Role, Match_Score, Location, Job_Type, HR_Name, HR_Email,
Recruiter_Name, Recruiter_Linkedin, Company_Website, Company_Size,
Tech_Stack, Job_Description, Why_Match, Suggested_Subject, Application_Link
```

Example row:
```csv
Stripe,Backend Engineer,92,Remote,Full-time,Sarah Chen,sarah@stripe.com,,,,500-1000,"Go, Java, Kubernetes","Build payment infrastructure","Strong distributed systems background","Re: Backend Engineer at Stripe",https://stripe.com/jobs/123
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/resume/upload` | Upload + parse PDF resume |
| POST | `/api/jobs/upload` | Import jobs CSV |
| GET  | `/api/jobs` | List all jobs |
| POST | `/api/emails/generate/{jobId}` | AI generate email for one job |
| POST | `/api/emails/generate/all` | Generate for all pending jobs |
| PATCH | `/api/emails/{id}/approve` | Approve email |
| POST | `/api/emails/{id}/send` | Send email with resume attached |
| POST | `/api/emails/send/batch` | Send all approved emails |
| GET  | `/api/dashboard/stats` | Dashboard stats |
| GET  | `/api/settings` | Get user settings |
| PUT  | `/api/settings` | Update settings |

---

## Architecture

```
frontend (Next.js 15)  →  backend (Spring Boot 3)  →  MySQL
                                    ↓                     ↓
                              Ollama (local AI)     Resume uploads
                                    ↓
                              Gmail SMTP/IMAP
```

**Schedulers (run every 15 min):**
- `InboxPollScheduler` — polls Gmail IMAP, classifies replies with AI
- `FollowUpScheduler` — sends Day 7 / Day 14 follow-ups, stops on any reply

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `mysql` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `outreach_db` | Database name |
| `DB_USER` | `outreach_user` | DB username |
| `DB_PASSWORD` | `outreach_pass` | DB password |
| `OLLAMA_URL` | `http://ollama:11434` | Ollama base URL |
| `OLLAMA_MODEL` | `qwen3` | Default model |
| `JWT_SECRET` | (change this!) | JWT signing secret |
| `UPLOAD_DIR` | `/app/uploads/resumes` | Resume storage path |
| `CREDENTIAL_KEY` | (change this!) | AES-256 key for encrypting stored passwords |
| `CORS_ORIGINS` | `http://localhost:3000` | Comma-separated allowed frontend origins |

---

## Production Deployment Checklist

- [ ] Change `JWT_SECRET` to a random 256-bit string
- [ ] Change all database passwords
- [ ] Set `NEXT_PUBLIC_API_URL` to your actual backend domain
- [ ] Configure HTTPS (Nginx reverse proxy recommended)
- [ ] Set up persistent volume backups for MySQL
- [ ] Pull Ollama model before first use
- [ ] Set Gmail App Password in Settings after first login

---

## Troubleshooting

**"AI generation failed. Is Ollama running?"**
→ Run `docker exec outreach-ollama ollama list` to verify model is pulled.

**"Please configure Gmail settings first"**
→ Go to Settings and save your Gmail address + App Password.

**Emails not sending**
→ Verify Gmail App Password is correct. Must be App Password, not account password.

**Backend won't start**
→ Check MySQL is healthy: `docker compose logs mysql`

**"No resume found"**
→ Upload your resume.pdf in the Upload page before generating emails.
