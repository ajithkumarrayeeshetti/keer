package com.outreach.controller;

import com.outreach.dto.request.EmailEditRequest;
import com.outreach.dto.response.*;
import com.outreach.entity.*;
import com.outreach.repository.UserRepository;
import com.outreach.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailGenerationService emailGenService;
    private final EmailSendingService    emailSendService;
    private final UserRepository         userRepository;

    @PostMapping("/generate/{jobId}")
    public ResponseEntity<ApiResponse<EmailPreviewResponse>> generate(
            @PathVariable Long jobId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(emailGenService.generateForJob(jobId, getUser(ud))));
    }

    @PostMapping("/generate/all")
    public ResponseEntity<ApiResponse<List<EmailPreviewResponse>>> generateAll(
            @AuthenticationPrincipal UserDetails ud) {
        List<EmailPreviewResponse> results = emailGenService.generateForAllPending(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success(results.size() + " emails generated", results));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<EmailPreviewResponse>> getPreview(
            @PathVariable Long jobId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(emailGenService.getPreview(jobId, getUser(ud))));
    }

    /**
     * Full rendered preview — includes signature and resume filename,
     * exactly as the HR will receive it. Used by the "Preview before send" modal.
     */
    @GetMapping("/{emailId}/full-preview")
    public ResponseEntity<ApiResponse<EmailSendingService.EmailPreviewFull>> getFullPreview(
            @PathVariable Long emailId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(emailSendService.buildFullPreview(emailId, getUser(ud))));
    }

    @PutMapping("/{emailId}")
    public ResponseEntity<ApiResponse<Email>> edit(
            @PathVariable Long emailId,
            @RequestBody EmailEditRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                emailGenService.editEmail(emailId, req.getSubject(), req.getBody(), getUser(ud))));
    }

    @PatchMapping("/{emailId}/approve")
    public ResponseEntity<ApiResponse<Email>> approve(
            @PathVariable Long emailId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(emailGenService.approveEmail(emailId, getUser(ud))));
    }

    @PatchMapping("/{emailId}/skip")
    public ResponseEntity<ApiResponse<Void>> skip(
            @PathVariable Long emailId, @AuthenticationPrincipal UserDetails ud) {
        emailGenService.skipEmail(emailId, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Email skipped", null));
    }

    @PostMapping("/{emailId}/send")
    public ResponseEntity<ApiResponse<Application>> send(
            @PathVariable Long emailId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(emailSendService.sendEmail(emailId, getUser(ud))));
    }

    @PostMapping("/send/batch")
    public ResponseEntity<ApiResponse<List<Application>>> sendBatch(
            @AuthenticationPrincipal UserDetails ud) {
        List<Application> sent = emailSendService.sendBatch(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success(sent.size() + " emails sent", sent));
    }

    /** Retry all FAILED emails for this user (up to 3 attempts each). */
    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<Void>> retryFailed(
            @AuthenticationPrincipal UserDetails ud) {
        emailSendService.retryFailures(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Retry initiated", null));
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }
}
