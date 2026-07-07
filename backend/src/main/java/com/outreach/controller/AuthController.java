package com.outreach.controller;

import com.outreach.dto.request.LoginRequest;
import com.outreach.dto.request.RegisterRequest;
import com.outreach.dto.response.ApiResponse;
import com.outreach.dto.response.AuthResponse;
import com.outreach.entity.User;
import com.outreach.repository.UserRepository;
import com.outreach.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtTokenProvider   jwtProvider;
    private final AuthenticationManager authManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("An account with this email already exists"));
        }
        if (req.getPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 8 characters"));
        }

        User user = User.builder()
                .name(req.getName().trim())
                .email(req.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Account created",
                new AuthResponse(token, user.getName(), user.getEmail())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getEmail().trim().toLowerCase(),
                            req.getPassword()));
            String token = jwtProvider.generateToken(auth);
            User user = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                    .orElseThrow();
            return ResponseEntity.ok(ApiResponse.success("Login successful",
                    new AuthResponse(token, user.getName(), user.getEmail())));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid email or password"));
        } catch (DisabledException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Account is disabled"));
        }
    }
}
