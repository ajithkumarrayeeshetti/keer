package com.outreach.service;

import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final AiProviderService  aiProviderService;
    private final PromptBuilder      promptBuilder;

    public void scheduleFollowUps(Application application, Settings settings, Resume resume, Job job) {
        if (application.getSentAt() == null) {
            log.warn("Application {} has null sentAt — using now() for follow-up scheduling", application.getId());
            application.setSentAt(LocalDateTime.now());
        }
        int day1 = settings.getFollowupDay1() != null ? settings.getFollowupDay1() : 7;
        int day2 = settings.getFollowupDay2() != null ? settings.getFollowupDay2() : 14;
        scheduleOne(application, resume, job, 1, application.getSentAt().plusDays(day1));
        scheduleOne(application, resume, job, 2, application.getSentAt().plusDays(day2));
    }

    private void scheduleOne(Application app, Resume resume, Job job, int seq, LocalDateTime when) {
        try {
            String prompt = promptBuilder.buildFollowUpPrompt(resume, job, seq);
            String raw    = aiProviderService.generate(prompt, app.getUser().getId());

            // Strip reasoning-model think blocks
            String aiOutput = raw.replaceAll("(?s)<think>.*?</think>", "").trim();

            String subject = extractSection(aiOutput, "SUBJECT:", "BODY:").trim();
            String body    = extractAfter(aiOutput, "BODY:").trim();

            // Fallback if model didn't use the expected format
            if (subject.isBlank()) subject = "Following up on my application – " + job.getRole();
            if (body.isBlank())    body    = aiOutput;

            FollowUp followUp = FollowUp.builder()
                    .application(app)
                    .user(app.getUser())
                    .sequenceNumber(seq)
                    .subject(subject)
                    .body(body)
                    .scheduledAt(when)
                    .status(FollowUp.FollowUpStatus.PENDING)
                    .build();
            followUpRepository.save(followUp);
            log.debug("Scheduled follow-up #{} for application {} at {}", seq, app.getId(), when);
        } catch (Exception e) {
            log.error("Could not schedule follow-up {} for application {}: {}", seq, app.getId(), e.getMessage());
        }
    }

    public void cancelFollowUps(Application application) {
        List<FollowUp> pending = followUpRepository.findByApplicationIdAndStatus(
                application.getId(), FollowUp.FollowUpStatus.PENDING);
        if (pending.isEmpty()) return;
        pending.forEach(f -> f.setStatus(FollowUp.FollowUpStatus.CANCELLED));
        followUpRepository.saveAll(pending);
        log.info("Cancelled {} follow-up(s) for application {}", pending.size(), application.getId());
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
