package com.outreach.service;

import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.util.PromptBuilder;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxMonitorService {

    private final ReplyRepository       replyRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository         jobRepository;
    private final FollowUpService       followUpService;
    private final AiProviderService     aiProviderService;
    private final PromptBuilder         promptBuilder;
    private final SettingsRepository    settingsRepository;
    private final CredentialEncryptionService encryptionService;

    public void pollInbox(User user) {
        Settings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
        if (settings == null || settings.getGmailAddress() == null
                || settings.getGmailAppPassword() == null) return;

        String password = encryptionService.decrypt(settings.getGmailAppPassword());
        if (password == null || password.isBlank()) return;

        Store  store = null;
        Folder inbox = null;
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol",      "imaps");
            props.put("mail.imaps.host",          "imap.gmail.com");
            props.put("mail.imaps.port",          "993");
            props.put("mail.imaps.ssl.enable",    "true");
            props.put("mail.imaps.connectiontimeout", "10000");
            props.put("mail.imaps.timeout",           "10000");

            store = Session.getInstance(props).getStore("imaps");
            store.connect("imap.gmail.com", settings.getGmailAddress(), password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Inbox poll for user {}: {} unseen message(s)", user.getId(), messages.length);

            for (Message message : messages) {
                try {
                    processMessage(message, user);
                } catch (Exception e) {
                    log.warn("Skipping message: {}", e.getMessage());
                } finally {
                    try { message.setFlag(Flags.Flag.SEEN, true); } catch (Exception ignored) {}
                }
            }
        } catch (AuthenticationFailedException e) {
            log.warn("IMAP auth failed for user {} — check App Password", user.getId());
        } catch (Exception e) {
            log.error("Inbox poll error for user {}: {}", user.getId(), e.getMessage());
        } finally {
            try { if (inbox != null && inbox.isOpen()) inbox.close(false); } catch (Exception ignored) {}
            try { if (store != null && store.isConnected()) store.close();  } catch (Exception ignored) {}
        }
    }

    private void processMessage(Message message, User user) throws Exception {
        String messageId = getHeader(message, "Message-ID");
        if (messageId != null && replyRepository.existsByMessageId(messageId)) return;

        Address[] froms = message.getFrom();
        if (froms == null || froms.length == 0) return;
        String from    = ((InternetAddress) froms[0]).getAddress();
        String subject = message.getSubject();
        String body    = extractText(message);

        Application application = matchApplication(user, from, subject);
        if (application == null) return;

        String classification = classifyReply(body, user.getId());

        replyRepository.save(Reply.builder()
                .application(application).user(user)
                .messageId(messageId).fromAddress(from)
                .subject(subject).body(truncate(body, 10000))
                .classification(Reply.ReplyClassification.valueOf(classification))
                .receivedAt(LocalDateTime.now())
                .build());

        switch (classification) {
            case "INTERVIEW" -> {
                application.setStatus(Application.ApplicationStatus.INTERVIEW);
                application.getJob().setStatus(Job.JobStatus.REPLIED);
            }
            case "POSITIVE" -> {
                application.setStatus(Application.ApplicationStatus.REPLIED_POSITIVE);
                application.getJob().setStatus(Job.JobStatus.REPLIED);
            }
            case "REJECTION" -> {
                application.setStatus(Application.ApplicationStatus.REJECTED);
                application.getJob().setStatus(Job.JobStatus.REJECTED);
            }
            default -> { /* NEUTRAL / UNKNOWN — no change */ }
        }
        // Persist both application and job status
        applicationRepository.save(application);
        jobRepository.save(application.getJob());

        if (!classification.equals("UNKNOWN") && !classification.equals("NEUTRAL")) {
            followUpService.cancelFollowUps(application);
        }
    }

    private Application matchApplication(User user, String from, String subject) {
        List<Application> apps = applicationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        // Primary: exact email match (case-insensitive)
        return apps.stream()
                .filter(a -> a.getHrEmail() != null
                        && from.trim().equalsIgnoreCase(a.getHrEmail().trim()))
                .findFirst()
                .orElseGet(() ->
                    // Fallback: company name in subject
                    apps.stream()
                        .filter(a -> subject != null && a.getCompany() != null
                                && subject.toLowerCase().contains(a.getCompany().toLowerCase()))
                        .findFirst().orElse(null));
    }

    private String classifyReply(String body, Long userId) {
        try {
            String prompt = promptBuilder.buildReplyClassificationPrompt(body);
            String result = aiProviderService.generate(prompt, userId)
                    .toUpperCase().replaceAll("[^A-Z_]", "").trim();
            List<String> valid = List.of("POSITIVE","INTERVIEW","REJECTION","NEUTRAL","UNKNOWN");
            return valid.contains(result) ? result : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Reply classification failed: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private String getHeader(Message msg, String name) {
        try {
            String[] h = msg.getHeader(name);
            return h != null && h.length > 0 ? h[0] : null;
        } catch (Exception e) { return null; }
    }

    private String extractText(Message msg) {
        try {
            if (msg.isMimeType("text/plain")) return (String) msg.getContent();
            if (msg.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) msg.getContent();
                // Prefer plain text
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart p = mp.getBodyPart(i);
                    if (p.isMimeType("text/plain")) return (String) p.getContent();
                }
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart p = mp.getBodyPart(i);
                    if (p.isMimeType("text/html")) return p.getContent().toString();
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Could not extract text: {}", e.getMessage());
            return "";
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : (s != null ? s : "");
    }
}
