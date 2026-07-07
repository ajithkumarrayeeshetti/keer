package com.outreach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.entity.Resume;
import com.outreach.entity.User;
import com.outreach.repository.ResumeRepository;
import com.outreach.util.PdfParser;
import com.outreach.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final PdfParser pdfParser;
    private final AiProviderService aiProviderService;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.resume-dir}")
    private String resumeDir;

    public Resume uploadAndParse(MultipartFile file, User user) throws IOException {
        // Validate file type
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted for resumes");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Resume file must be under 10 MB");
        }

        // Save file to disk first — MultipartFile InputStream can only be read once
        Path uploadPath = Paths.get(resumeDir);
        Files.createDirectories(uploadPath);
        String filename = UUID.randomUUID() + "_" + originalName;
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Parse text from saved file (not from already-consumed stream)
        String rawText;
        try {
            rawText = pdfParser.extractText(filePath.toString());
        } catch (Exception e) {
            Files.deleteIfExists(filePath); // clean up on parse failure
            throw new IOException("Could not parse PDF text: " + e.getMessage(), e);
        }

        if (rawText == null || rawText.isBlank()) {
            Files.deleteIfExists(filePath);
            throw new IllegalArgumentException("Could not extract any text from the PDF. Is it a scanned image?");
        }

        // AI extraction
        String prompt = promptBuilder.buildResumeExtractionPrompt(rawText);
        String aiResponse = aiProviderService.generate(prompt, user.getId());

        Resume resume = Resume.builder()
                .user(user)
                .filename(originalName)
                .filePath(filePath.toString())
                .rawText(rawText)
                .parsedAt(LocalDateTime.now())
                .skills("[]").technologies("[]").projects("[]").experience("[]").education("[]")
                .build();

        try {
            String clean = aiResponse.trim().replaceAll("(?s)```json|```", "").trim();
            // Strip optional <think>...</think> blocks from reasoning models (e.g. qwen3)
            clean = clean.replaceAll("(?s)<think>.*?</think>", "").trim();
            Map<?, ?> parsed = objectMapper.readValue(clean, Map.class);
            if (parsed.get("skills") != null)       resume.setSkills(objectMapper.writeValueAsString(parsed.get("skills")));
            if (parsed.get("technologies") != null) resume.setTechnologies(objectMapper.writeValueAsString(parsed.get("technologies")));
            if (parsed.get("projects") != null)     resume.setProjects(objectMapper.writeValueAsString(parsed.get("projects")));
            if (parsed.get("experience") != null)   resume.setExperience(objectMapper.writeValueAsString(parsed.get("experience")));
            if (parsed.get("education") != null)    resume.setEducation(objectMapper.writeValueAsString(parsed.get("education")));
        } catch (Exception e) {
            log.warn("AI resume extraction parse error (raw text still saved): {}", e.getMessage());
        }

        // Delete previous resume file + record for this user
        resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).ifPresent(old -> {
            try { Files.deleteIfExists(Paths.get(old.getFilePath())); } catch (Exception ignored) {}
            resumeRepository.delete(old);
        });

        return resumeRepository.save(resume);
    }

    public Resume getResumeByUser(User user) {
        return resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new RuntimeException("No resume found. Please upload your resume first."));
    }
}
