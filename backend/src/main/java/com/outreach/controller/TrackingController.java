package com.outreach.controller;

import com.outreach.entity.Email;
import com.outreach.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Serves a 1×1 transparent GIF when the HR opens the email.
 * The unique tracking token in the URL identifies which email was opened.
 *
 * Endpoint is intentionally unauthenticated — it's called by the HR's
 * email client, not by the authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingController {

    // Smallest valid 1×1 transparent GIF (35 bytes)
    private static final byte[] PIXEL_GIF = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final EmailRepository emailRepository;

    @GetMapping("/open/{token}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable String token) {
        try {
            emailRepository.findByTrackingToken(token).ifPresent(email -> {
                if (email.getOpenedAt() == null) {
                    email.setOpenedAt(LocalDateTime.now());
                    emailRepository.save(email);
                    log.info("Email {} opened by HR (token={})", email.getId(), token);
                }
            });
        } catch (Exception e) {
            log.warn("Tracking pixel error for token {}: {}", token, e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        return new ResponseEntity<>(PIXEL_GIF, headers, HttpStatus.OK);
    }
}
