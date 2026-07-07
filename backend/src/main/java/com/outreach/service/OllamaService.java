package com.outreach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    @Qualifier("ollamaWebClient")
    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.default-model}")
    private String defaultModel;

    @Value("${app.ollama.timeout}")
    private int timeoutSeconds;

    public String generate(String prompt) {
        return generate(prompt, defaultModel);
    }

    public String generate(String prompt, String model) {
        try {
            Map<String, Object> request = Map.of(
                "model",   model,
                "prompt",  prompt,
                "stream",  false,
                "options", Map.of("temperature", 0.7, "top_p", 0.9, "num_predict", 600)
            );

            String responseBody = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode responseNode = json.get("response");
            if (responseNode == null || responseNode.isNull()) {
                throw new RuntimeException("Ollama returned empty response for model: " + model);
            }
            // Strip reasoning-model think blocks
            return responseNode.asText().trim()
                    .replaceAll("(?s)<think>.*?</think>", "").trim();

        } catch (WebClientResponseException e) {
            log.error("Ollama HTTP {} for model {}: {}", e.getStatusCode(), model, e.getResponseBodyAsString());
            throw new RuntimeException("Ollama error (HTTP " + e.getStatusCode() + "): " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ollama generation failed for model {}: {}", model, e.getMessage());
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        try {
            ollamaWebClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listModels() {
        try {
            String response = ollamaWebClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            JsonNode json = objectMapper.readTree(response);
            JsonNode models = json.get("models");
            if (models == null || !models.isArray()) return List.of();
            return models.findValuesAsText("name");
        } catch (Exception e) {
            log.error("Could not list Ollama models: {}", e.getMessage());
            return List.of();
        }
    }
}
