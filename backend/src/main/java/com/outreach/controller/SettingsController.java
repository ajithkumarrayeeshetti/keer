package com.outreach.controller;

import com.outreach.dto.response.ApiResponse;
import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.service.AiProviderService;
import com.outreach.service.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AiProviderService aiProviderService;
    private final CredentialEncryptionService encryptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Settings>> get(@AuthenticationPrincipal UserDetails ud) {
        User user = getUser(ud);
        Settings settings = settingsRepository.findByUserId(user.getId())
                .orElse(Settings.builder().user(user).build());
        // Always mask secrets in API responses — never send raw or decrypted values
        if (settings.getGmailAppPassword() != null) settings.setGmailAppPassword("***");
        if (settings.getAiApiKey() != null && !settings.getAiApiKey().isBlank())
            settings.setAiApiKey(settings.getAiApiKey().substring(0, Math.min(6, settings.getAiApiKey().length())) + "***");
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Settings>> update(
            @RequestBody Settings incoming,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getUser(ud);
        Settings s = settingsRepository.findByUserId(user.getId())
                .orElse(Settings.builder().user(user).build());

        if (incoming.getGmailAddress() != null)     s.setGmailAddress(incoming.getGmailAddress());

        // Encrypt credentials before storing
        if (incoming.getGmailAppPassword() != null && !incoming.getGmailAppPassword().equals("***"))
            s.setGmailAppPassword(encryptionService.encrypt(incoming.getGmailAppPassword()));

        if (incoming.getAiProvider() != null)       s.setAiProvider(incoming.getAiProvider());

        if (incoming.getAiApiKey() != null && !incoming.getAiApiKey().contains("***"))
            s.setAiApiKey(encryptionService.encrypt(incoming.getAiApiKey()));

        if (incoming.getAiModel() != null)          s.setAiModel(incoming.getAiModel());
        if (incoming.getOllamaModel() != null)      s.setOllamaModel(incoming.getOllamaModel());
        if (incoming.getOllamaUrl() != null)        s.setOllamaUrl(incoming.getOllamaUrl());
        if (incoming.getFollowupDay1() != null)     s.setFollowupDay1(incoming.getFollowupDay1());
        if (incoming.getFollowupDay2() != null)     s.setFollowupDay2(incoming.getFollowupDay2());
        if (incoming.getEmailSignature() != null)   s.setEmailSignature(incoming.getEmailSignature());
        if (incoming.getBatchSendDelaySeconds() != null) s.setBatchSendDelaySeconds(incoming.getBatchSendDelaySeconds());

        Settings saved = settingsRepository.save(s);
        saved.setGmailAppPassword("***");
        return ResponseEntity.ok(ApiResponse.success("Settings saved", saved));
    }

    @PostMapping("/test-ai")
    public ResponseEntity<ApiResponse<String>> testAi(@AuthenticationPrincipal UserDetails ud) {
        User user = getUser(ud);
        try {
            String result = aiProviderService.generate("Say 'AI connection successful' in exactly 4 words.", user.getId());
            return ResponseEntity.ok(ApiResponse.success("Connection successful", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Connection failed: " + e.getMessage()));
        }
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }
}
