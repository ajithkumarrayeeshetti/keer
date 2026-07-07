package com.outreach.service;

import com.outreach.dto.response.EmailPreviewResponse;
import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailGenerationService {

    private final EmailRepository emailRepository;
    private final JobRepository jobRepository;
    private final AiProviderService aiProviderService;
    private final PromptBuilder promptBuilder;
    private final ResumeService resumeService;

    public EmailPreviewResponse generateForJob(Long jobId, User user) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        Resume resume = resumeService.getResumeByUser(user);
        return generateEmail(job, resume, user);
    }

    public List<EmailPreviewResponse> generateForAllPending(User user) {
        Resume resume = resumeService.getResumeByUser(user);
        List<Job> pendingJobs = jobRepository.findByUserIdAndStatus(user.getId(), Job.JobStatus.PENDING);
        List<EmailPreviewResponse> results = new ArrayList<>();
        for (Job job : pendingJobs) {
            try {
                results.add(generateEmail(job, resume, user));
            } catch (Exception e) {
                log.error("Failed to generate email for job {}: {}", job.getId(), e.getMessage());
            }
        }
        return results;
    }

    private EmailPreviewResponse generateEmail(Job job, Resume resume, User user) {
        Email existing = emailRepository.findByJobId(job.getId()).orElse(null);
        if (existing != null && existing.getStatus() != Email.EmailStatus.DRAFT) {
            return toPreviewResponse(existing, job);
        }

        String prompt = promptBuilder.buildEmailGenerationPrompt(resume, job);
        String aiOutput = aiProviderService.generate(prompt, user.getId());

        // Strip reasoning-model thinking blocks before extracting subject/body
        aiOutput = aiOutput.replaceAll("(?s)<think>.*?</think>", "").trim();

        String subject = extractSection(aiOutput, "SUBJECT:", "BODY:").trim();
        String body    = extractAfter(aiOutput, "BODY:").trim();

        // Fallback if AI didn't use the expected format
        if (subject.isEmpty() && job.getSuggestedSubject() != null) {
            subject = job.getSuggestedSubject();
        }
        if (body.isEmpty()) body = aiOutput; // last resort: use entire output as body

        Email email = existing != null ? existing : Email.builder().user(user).job(job).build();
        email.setSubject(subject);
        email.setBody(body);
        email.setStatus(Email.EmailStatus.DRAFT);
        emailRepository.save(email);

        job.setStatus(Job.JobStatus.EMAIL_GENERATED);
        jobRepository.save(job);

        return toPreviewResponse(email, job);
    }

    public Email approveEmail(Long emailId, User user) {
        Email email = getEmailById(emailId, user);
        email.setStatus(Email.EmailStatus.APPROVED);
        return emailRepository.save(email);
    }

    public Email editEmail(Long emailId, String subject, String body, User user) {
        Email email = getEmailById(emailId, user);
        if (subject != null && !subject.isBlank()) email.setSubject(subject);
        if (body    != null && !body.isBlank())    email.setBody(body);
        return emailRepository.save(email);
    }

    public void skipEmail(Long emailId, User user) {
        Email email = getEmailById(emailId, user);
        Job job = email.getJob();
        job.setStatus(Job.JobStatus.SKIPPED);
        jobRepository.save(job);
        emailRepository.delete(email);
    }

    public EmailPreviewResponse getPreview(Long jobId, User user) {
        Email email = emailRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("No email generated for this job yet"));
        if (!email.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        return toPreviewResponse(email, email.getJob());
    }

    private Email getEmailById(Long emailId, User user) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        if (!email.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        return email;
    }

    private EmailPreviewResponse toPreviewResponse(Email email, Job job) {
        return EmailPreviewResponse.builder()
                .emailId(email.getId())
                .jobId(job.getId())
                .company(job.getCompany())
                .role(job.getRole())
                .hrName(job.getHrName())
                .hrEmail(job.getHrEmail())
                .subject(email.getSubject())
                .body(email.getBody())
                .status(email.getStatus().name())
                .openedAt(email.getOpenedAt() != null ? email.getOpenedAt().toString() : null)
                .build();
    }

    private String extractSection(String text, String start, String end) {
        int s = text.indexOf(start);
        int e = text.indexOf(end);
        if (s < 0) return "";
        s += start.length();
        return (e > s) ? text.substring(s, e) : text.substring(s);
    }

    private String extractAfter(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return "";
        return text.substring(idx + marker.length());
    }
}
