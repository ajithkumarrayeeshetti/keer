package com.outreach.controller;

import com.outreach.dto.response.ApiResponse;
import com.outreach.entity.*;
import com.outreach.repository.UserRepository;
import com.outreach.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            JobService.UploadResult result = jobService.uploadJobs(file, getUser(ud));
            String message = result.jobs().size() + " jobs imported"
                    + (result.errors().isEmpty() ? "" : " (" + result.errors().size() + " row(s) skipped)");
            Map<String, Object> body = Map.of(
                    "imported", result.jobs().size(),
                    "jobs",     result.jobs(),
                    "errors",   result.errors());
            return ResponseEntity.ok(ApiResponse.success(message, body));
        } catch (IllegalArgumentException e) {
            // Missing required columns — return a 400 with the exact message
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("CSV import failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Job>>> list(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobsByUser(getUser(ud))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getOne(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobById(id, getUser(ud))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Job>> updateStatus(
            @PathVariable Long id,
            @RequestParam Job.JobStatus status,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(jobService.updateJobStatus(id, status, getUser(ud))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        jobService.deleteJob(id, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Job deleted", null));
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }
}
