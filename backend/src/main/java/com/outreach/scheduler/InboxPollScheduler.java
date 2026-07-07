package com.outreach.scheduler;

import com.outreach.entity.Settings;
import com.outreach.repository.SettingsRepository;
import com.outreach.service.InboxMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxPollScheduler {

    private final SettingsRepository settingsRepository;
    private final InboxMonitorService inboxMonitorService;

    /** Poll every 10 minutes. Only polls users with Gmail configured. */
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 60 * 1000)
    public void pollAllInboxes() {
        List<Settings> configured = settingsRepository.findAll().stream()
                .filter(s -> s.getGmailAddress() != null && !s.getGmailAddress().isBlank()
                          && s.getGmailAppPassword() != null && !s.getGmailAppPassword().isBlank())
                .toList();

        log.debug("InboxPollScheduler: polling {} configured user(s)", configured.size());

        for (Settings settings : configured) {
            try {
                inboxMonitorService.pollInbox(settings.getUser());
            } catch (Exception e) {
                log.warn("Inbox poll failed for user {}: {}", settings.getUser().getId(), e.getMessage());
            }
        }
    }
}
