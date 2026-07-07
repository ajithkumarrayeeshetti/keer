package com.outreach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.entity.Settings;
import com.outreach.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final ObjectMapper objectMapper;
    private final SettingsRepository settingsRepository;
    private final CredentialEncryptionService encryptionService;

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    public String generate(String prompt, Long userId) {
        Settings settings = settingsRepository.findByUserId(userId).orElse(null);
        if (settings == null) return generateWithOllama(prompt, "qwen3", "http://localhost:11434");

        String provider = settings.getAiProvider() != null ? settings.getAiProvider().toUpperCase() : "OLLAMA";

        String raw = switch (provider) {
            case "GEMINI"   -> generateWithGemini(prompt,
                    decryptKey(settings.getAiApiKey()), settings.getAiModel());
            case "GROQ"     -> generateWithOpenAiCompat(prompt,
                    decryptKey(settings.getAiApiKey()),
                    hasValue(settings.getAiModel()) ? settings.getAiModel() : "llama-3.3-70b-versatile",
                    "https://api.groq.com/openai/v1/chat/completions");
            case "TOGETHER" -> generateWithOpenAiCompat(prompt,
                    decryptKey(settings.getAiApiKey()),
                    hasValue(settings.getAiModel()) ? settings.getAiModel() : "mistralai/Mixtral-8x7B-Instruct-v0.1",
                    "https://api.together.xyz/v1/chat/completions");
            case "OPENAI"   -> generateWithOpenAiCompat(prompt,
                    decryptKey(settings.getAiApiKey()),
                    hasValue(settings.getAiModel()) ? settings.getAiModel() : "gpt-4o-mini",
                    "https://api.openai.com/v1/chat/completions");
            default         -> generateWithOllama(prompt,
                    hasValue(settings.getOllamaModel()) ? settings.getOllamaModel() : "qwen3",
                    hasValue(settings.getOllamaUrl())   ? settings.getOllamaUrl()   : "http://ollama:11434");
        };
        return raw.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    /** Returns true only if value is non-null and non-blank. */
    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private String decryptKey(String stored) {
        if (stored == null || stored.isBlank()) return "";
        return encryptionService.decrypt(stored);
    }

    private String generateWithGemini(String prompt, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) throw new RuntimeException("Gemini API key not configured");
        String m = hasValue(model) ? model : "gemini-1.5-flash";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + m
                + ":generateContent?key=" + apiKey;
        try {
            Map<String, Object> body = Map.of(
                "contents",         List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 700, "temperature", 0.7)
            );
            String response = WebClient.create().post().uri(url)
                    .bodyValue(body).retrieve().bodyToMono(String.class)
                    .timeout(TIMEOUT).block();
            return objectMapper.readTree(response)
                    .at("/candidates/0/content/parts/0/text").asText().trim();
        } catch (Exception e) {
            log.error("Gemini error: {}", e.getMessage());
            throw new RuntimeException("Gemini generation failed: " + e.getMessage(), e);
        }
    }

    /** Shared handler for Groq, Together AI, and OpenAI (all OpenAI-compatible). */
    private String generateWithOpenAiCompat(String prompt, String apiKey, String model, String url) {
        if (apiKey == null || apiKey.isBlank())
            throw new RuntimeException("API key not configured for provider: " + url);
        log.info("Calling AI provider: {} with model: {}", url, model);
        try {
            Map<String, Object> body = Map.of(
                "model",       model,
                "messages",    List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens",  700,
                "temperature", 0.7
            );
            String response = WebClient.create().post().uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body).retrieve().bodyToMono(String.class)
                    .timeout(TIMEOUT).block();
            return objectMapper.readTree(response)
                    .at("/choices/0/message/content").asText().trim();
        } catch (Exception e) {
            log.error("OpenAI-compat error ({}): {}", url, e.getMessage());
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    public String generateWithOllama(String prompt, String model, String baseUrl) {
        try {
            Map<String, Object> body = Map.of(
                "model",   model,
                "prompt",  prompt,
                "stream",  false,
                "options", Map.of("temperature", 0.7, "num_predict", 700)
            );
            String response = WebClient.create().post()
                    .uri(baseUrl + "/api/generate")
                    .bodyValue(body).retrieve().bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120)).block();
            JsonNode node = objectMapper.readTree(response).get("response");
            if (node == null) throw new RuntimeException("Empty response from Ollama");
            return node.asText().trim();
        } catch (Exception e) {
            log.error("Ollama error: {}", e.getMessage());
            throw new RuntimeException("Ollama generation failed: " + e.getMessage(), e);
        }
    }
}