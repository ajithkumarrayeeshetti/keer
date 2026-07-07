package com.outreach.service;

import com.outreach.entity.*;
import com.outreach.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendingService {

    private final EmailRepository       emailRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository         jobRepository;
    private final FollowUpService       followUpService;
    private final SettingsRepository    settingsRepository;
    private final ResumeService         resumeService;
    private final CredentialEncryptionService encryptionService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public Application sendEmail(Long emailId, User user) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        if (!email.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        if (email.getStatus() == Email.EmailStatus.SENT)   throw new RuntimeException("Email already sent");

        Settings settings = settingsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Please configure Gmail settings first"));
        validateGmailSettings(settings);

        Resume resume = resumeService.getResumeByUser(user);
        Job job = email.getJob();

        try {
            MimeMessage message = buildMailSender(settings).createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(settings.getGmailAddress());
            helper.setTo(job.getHrEmail());
            helper.setSubject(email.getSubject());

            String body = email.getBody();
            if (settings.getEmailSignature() != null && !settings.getEmailSignature().isBlank()) {
                body += "\n\n--\n" + settings.getEmailSignature();
            }

            String trackingPixelUrl = baseUrl + "/api/track/open/" + email.getTrackingToken();
            String htmlBody = "<pre style='font-family:inherit;white-space:pre-wrap'>"
                    + escapeHtml(body) + "</pre>"
                    + "<img src='" + trackingPixelUrl
                    + "' width='1' height='1' style='display:none' alt='' />";
            helper.setText(body, htmlBody);

            File resumeFile = new File(resume.getFilePath());
            if (resumeFile.exists()) {
                helper.addAttachment(resume.getFilename(), new FileSystemResource(resumeFile));
            }

            buildMailSender(settings).send(message);

            email.setStatus(Email.EmailStatus.SENT);
            email.setSentAt(LocalDateTime.now());
            emailRepository.save(email);

            // Persist job status change
            job.setStatus(Job.JobStatus.SENT);
            jobRepository.save(job);

            Application application = Application.builder()
                    .user(user).job(job).email(email)
                    .company(job.getCompany()).role(job.getRole())
                    .hrName(job.getHrName()).hrEmail(job.getHrEmail())
                    .subject(email.getSubject()).emailContent(email.getBody())
                    .sentAt(LocalDateTime.now())
                    .status(Application.ApplicationStatus.SENT)
                    .build();

            Application saved = applicationRepository.save(application);
            followUpService.scheduleFollowUps(saved, settings, resume, job);
            return saved;

        } catch (Exception e) {
            email.setStatus(Email.EmailStatus.FAILED);
            email.setRetryCount(email.getRetryCount() == null ? 1 : email.getRetryCount() + 1);
            emailRepository.save(email);
            log.error("Failed to send email for job {}: {}", job.getId(), e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public List<Application> sendBatch(User user) {
        List<Email> approved = emailRepository.findByUserIdAndStatus(user.getId(), Email.EmailStatus.APPROVED);
        if (approved.isEmpty()) return List.of();

        Settings settings = settingsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Please configure Gmail settings first"));
        validateGmailSettings(settings);

        int delaySecs = settings.getBatchSendDelaySeconds() != null
                ? Math.max(settings.getBatchSendDelaySeconds(), 10) : 45;

        List<Application> results = new ArrayList<>();
        for (int i = 0; i < approved.size(); i++) {
            if (i > 0) {
                try {
                    log.info("Batch send: waiting {}s before email #{}", delaySecs, i + 1);
                    Thread.sleep(delaySecs * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch send interrupted at email #{}", i + 1);
                    break;
                }
            }
            try {
                Application sent = sendEmail(approved.get(i).getId(), user);
                if (sent != null) results.add(sent);
            } catch (Exception e) {
                log.error("Batch send failed for email {}: {}", approved.get(i).getId(), e.getMessage());
            }
        }
        return results;
    }

    public void retryFailures(User user) {
        emailRepository.findRetryableFailures().stream()
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .forEach(email -> {
                    try {
                        email.setStatus(Email.EmailStatus.APPROVED);
                        emailRepository.save(email);
                        sendEmail(email.getId(), user);
                        log.info("Retry succeeded for email {}", email.getId());
                    } catch (Exception e) {
                        log.warn("Retry failed for email {}: {}", email.getId(), e.getMessage());
                    }
                });
    }

    public record EmailPreviewFull(String subject, String bodyWithSignature, String resumeFilename) {}

    public EmailPreviewFull buildFullPreview(Long emailId, User user) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        if (!email.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");

        Settings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
        Resume resume;
        try { resume = resumeService.getResumeByUser(user); }
        catch (Exception e) { resume = null; }

        String body = email.getBody() != null ? email.getBody() : "";
        if (settings != null && settings.getEmailSignature() != null && !settings.getEmailSignature().isBlank()) {
            body += "\n\n--\n" + settings.getEmailSignature();
        }
        return new EmailPreviewFull(email.getSubject(), body, resume != null ? resume.getFilename() : null);
    }

    private void validateGmailSettings(Settings s) {
        if (s.getGmailAddress() == null || s.getGmailAddress().isBlank())
            throw new RuntimeException("Gmail address is not configured in Settings");
        if (s.getGmailAppPassword() == null || s.getGmailAppPassword().isBlank())
            throw new RuntimeException("Gmail App Password is not configured in Settings");
    }

    private JavaMailSenderImpl buildMailSender(Settings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(settings.getGmailAddress());
        sender.setPassword(encryptionService.decrypt(settings.getGmailAppPassword()));
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
