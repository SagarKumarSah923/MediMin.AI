package com.medimin.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medimin.common.ApiModels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@RestController
@RequestMapping("/internal/agent")
public class AgentController {
    private static final String SAFETY = "I’m here to provide general health information, not a diagnosis. If symptoms are severe, rapidly worsening, or feel like an emergency, contact local emergency services or a qualified healthcare professional.";
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String apiKey;
    private final String model;

    public AgentController(
            ObjectMapper mapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.mapper = mapper;
        this.http = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.model = model;
    }

    @PostMapping("/chat")
    public ApiModels.AgentChatResponse chat(@RequestBody ApiModels.AgentChatRequest request) {
        String fallback = "I can help you organize what you are noticing, think through useful questions, and identify when to seek professional care. Tell me what has changed, when it started, and how it is affecting your day.";
        if (apiKey == null || apiKey.isBlank()) return new ApiModels.AgentChatResponse(fallback);
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt()));
            messages.addAll(request.messages().stream().map(item -> Map.of(
                    "role", item.role(), "content", item.content()
            )).toList());
            Map<String, Object> payload = Map.of("model", model, "temperature", 0.2, "messages", messages);
            String body = mapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = mapper.readTree(response.body());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                if (!content.isBlank()) return new ApiModels.AgentChatResponse(content);
            }
        } catch (Exception ignored) {
            // Keep the assistant available with a cautious fallback when the provider is unavailable.
        }
        return new ApiModels.AgentChatResponse(fallback);
    }

    @PostMapping("/symptoms")
    public ApiModels.AgentSymptomResponse symptoms(@RequestBody ApiModels.AgentSymptomRequest request) {
        ApiModels.AgentSymptomResponse fallback = new ApiModels.AgentSymptomResponse(
                List.of("Common, non-specific causes can include stress, sleep changes, hydration, or a mild infection."),
                List.of("Notice whether symptoms improve, stay stable, or worsen", "Rest, hydrate, and avoid pushing through symptoms", "Contact a qualified clinician if symptoms persist or feel concerning"),
                request.severity() != null && request.severity().equalsIgnoreCase("severe") ? "Seek prompt professional guidance" : "Monitor and follow up if persistent",
                SAFETY
        );
        if (apiKey == null || apiKey.isBlank()) return fallback;
        try {
            String prompt = "Analyze these symptoms for general health information. Return JSON only with keys possibleCauses (array of 3 short strings), guidance (array of 3 short strings), urgency (short string), disclaimer (short string). Never diagnose. Symptoms: " + request.symptoms() + ". Duration: " + request.duration() + ". Severity: " + request.severity() + ". Notes: " + request.notes();
            Map<String, Object> payload = Map.of(
                    "model", model, "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt() + " Return strictly valid JSON for symptom analysis."),
                            Map.of("role", "user", "content", prompt)
                    )
            );
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content").asText();
                JsonNode json = mapper.readTree(stripFences(content));
                return new ApiModels.AgentSymptomResponse(
                        readStrings(json, "possibleCauses", fallback.possibleCauses()),
                        readStrings(json, "guidance", fallback.guidance()),
                        json.path("urgency").asText(fallback.urgency()),
                        json.path("disclaimer").asText(SAFETY)
                );
            }
        } catch (Exception ignored) {
            // Return the safe local analysis.
        }
        return fallback;
    }

    private String systemPrompt() {
        return "You are MediMin AI, a careful health information assistant. Do not diagnose, prescribe, or create false certainty. Ask clarifying questions when needed, explain uncertainty, encourage qualified clinical care for persistent or concerning symptoms, and direct emergencies to local emergency services. Be warm, concise, and practical.";
    }

    private static List<String> readStrings(JsonNode node, String field, List<String> fallback) {
        if (!node.has(field) || !node.path(field).isArray()) return fallback;
        List<String> values = new ArrayList<>();
        node.path(field).forEach(item -> values.add(item.asText()));
        return values.isEmpty() ? fallback : values;
    }

    private static String stripFences(String text) {
        return text.replace("```json", "").replace("```", "").trim();
    }
}