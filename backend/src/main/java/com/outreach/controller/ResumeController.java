package com.outreach.controller;

import com.outreach.dto.response.ApiResponse;
import com.outreach.entity.*;
import com.outreach.repository.UserRepository;
import com.outreach.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService    resumeService;
    private final UserRepository   userRepository;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Resume>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getUser(ud);
        try {
            Resume resume = resumeService.uploadAndParse(file, user);
            return ResponseEntity.ok(ApiResponse.success("Resume uploaded and parsed", resume));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to process resume: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Resume>> getResume(@AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(ApiResponse.success(resumeService.getResumeByUser(getUser(ud))));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.success(null)); // No resume yet — return null, not 500
        }
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }
}
