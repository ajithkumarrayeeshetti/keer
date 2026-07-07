package com.outreach.controller;

import com.outreach.dto.response.ApiResponse;
import com.outreach.entity.*;
import com.outreach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpRepository followUpRepository;
    private final UserRepository userRepository;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FollowUp>>> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = userRepository.findByEmail(ud.getUsername()).orElseThrow();
        List<FollowUp> all = followUpRepository.findByUserIdOrderByScheduledAtDesc(user.getId());
        return ResponseEntity.ok(ApiResponse.success(all));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails ud) {
        User user = userRepository.findByEmail(ud.getUsername()).orElseThrow();
        FollowUp fu = followUpRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Follow-up not found"));
        if (!fu.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        if (fu.getStatus() == FollowUp.FollowUpStatus.SENT)
            throw new RuntimeException("Cannot cancel an already-sent follow-up");
        fu.setStatus(FollowUp.FollowUpStatus.CANCELLED);
        followUpRepository.save(fu);
        return ResponseEntity.ok(ApiResponse.success("Cancelled", null));
    }
}
