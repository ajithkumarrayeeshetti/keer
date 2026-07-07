package com.outreach.scheduler;

import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.service.CredentialEncryptionService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpScheduler {

    private final FollowUpRepository    followUpRepository;
    private final SettingsRepository    settingsRepository;
    private final ResumeRepository      resumeRepository;
    private final CredentialEncryptionService encryptionService;

    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 2 * 60 * 1000)
    public void processFollowUps() {
        List<FollowUp> due = followUpRepository.findDueFollowUps(LocalDateTime.now());
        if (!due.isEmpty()) log.info("Processing {} due follow-up(s)", due.size());

        for (FollowUp followUp : due) {
            try {
                sendFollowUp(followUp);
                followUp.setStatus(FollowUp.FollowUpStatus.SENT);
                followUp.setSentAt(LocalDateTime.now());
                followUpRepository.save(followUp);
                log.info("Follow-up {} sent successfully", followUp.getId());
            } catch (Exception e) {
                log.error("Failed to send follow-up {}: {}", followUp.getId(), e.getMessage());
            }
        }
    }

    private void sendFollowUp(FollowUp followUp) throws Exception {
        User user = followUp.getUser();
        Settings settings = settingsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No settings for user " + user.getId()));

        if (settings.getGmailAddress() == null || settings.getGmailAppPassword() == null) {
            throw new RuntimeException("Gmail not configured for user " + user.getId());
        }

        Application app = followUp.getApplication();
        Resume resume   = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(settings.getGmailAddress());
        sender.setPassword(encryptionService.decrypt(settings.getGmailAppPassword()));
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(settings.getGmailAddress());
        helper.setTo(app.getHrEmail());
        helper.setSubject(followUp.getSubject() != null ? followUp.getSubject() : "Following up");

        String body = followUp.getBody() != null ? followUp.getBody() : "";
        if (settings.getEmailSignature() != null && !settings.getEmailSignature().isBlank()) {
            body += "\n\n--\n" + settings.getEmailSignature();
        }
        helper.setText(body);

        if (resume != null) {
            File resumeFile = new File(resume.getFilePath());
            if (resumeFile.exists()) {
                helper.addAttachment(resume.getFilename(), new FileSystemResource(resumeFile));
            }
        }

        sender.send(message);
    }
}
