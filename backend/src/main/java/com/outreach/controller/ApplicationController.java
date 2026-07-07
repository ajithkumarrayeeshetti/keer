package com.outreach.controller;

import com.outreach.dto.response.ApiResponse;
import com.outreach.entity.*;
import com.outreach.repository.*;
import com.outreach.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final FollowUpService followUpService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Application>>> list(@AuthenticationPrincipal UserDetails ud) {
        User user = userRepository.findByEmail(ud.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(
                applicationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Application>> get(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails ud) {
        Application app = getOwnedApp(id, ud);
        return ResponseEntity.ok(ApiResponse.success(app));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Application>> updateStatus(
            @PathVariable Long id,
            @RequestParam Application.ApplicationStatus status,
            @AuthenticationPrincipal UserDetails ud) {
        Application app = getOwnedApp(id, ud);
        app.setStatus(status);
        Application saved = applicationRepository.save(app);

        // Cancel pending follow-ups whenever the user manually marks as rejected
        if (status == Application.ApplicationStatus.REJECTED) {
            followUpService.cancelFollowUps(saved);
        }

        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    private Application getOwnedApp(Long id, UserDetails ud) {
        User user = userRepository.findByEmail(ud.getUsername()).orElseThrow();
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        return app;
    }
}
