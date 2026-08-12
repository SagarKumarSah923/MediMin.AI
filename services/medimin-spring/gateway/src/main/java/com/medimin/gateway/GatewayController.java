package com.medimin.gateway;

import com.medimin.common.ApiModels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private final RestClient client;
    private final String profileUrl;
    private final String careUrl;

    public GatewayController(
            RestClient client,
            @Value("${medimin.profile-service-url}") String profileUrl,
            @Value("${medimin.care-service-url}") String careUrl
    ) {
        this.client = client;
        this.profileUrl = profileUrl;
        this.careUrl = careUrl;
    }

    @GetMapping("/healthz")
    public ApiModels.HealthStatus health() {
        return new ApiModels.HealthStatus("ok");
    }

    @GetMapping("/profile")
    public ApiModels.Profile profile() {
        return get(profileUrl + "/internal/profile", ApiModels.Profile.class);
    }

    @GetMapping("/dashboard")
    public ApiModels.Dashboard dashboard() {
        return get(careUrl + "/internal/dashboard", ApiModels.Dashboard.class);
    }

    @GetMapping("/assessments")
    public ApiModels.Assessment[] assessments() {
        return get(careUrl + "/internal/assessments", ApiModels.Assessment[].class);
    }

    @PostMapping("/assessments")
    public ResponseEntity<ApiModels.Assessment> createAssessment(@RequestBody ApiModels.AssessmentInput input) {
        return ResponseEntity.status(201).body(post(careUrl + "/internal/assessments", input, ApiModels.Assessment.class));
    }

    @GetMapping("/assessments/{id}")
    public ApiModels.Assessment assessment(@PathVariable long id) {
        return get(careUrl + "/internal/assessments/" + id, ApiModels.Assessment.class);
    }

    @PostMapping("/symptom-checks")
    public ResponseEntity<ApiModels.SymptomCheck> symptomCheck(@RequestBody ApiModels.SymptomCheckInput input) {
        return ResponseEntity.status(201).body(post(careUrl + "/internal/symptom-checks", input, ApiModels.SymptomCheck.class));
    }

    @GetMapping("/conversations")
    public ApiModels.Conversation[] conversations() {
        return get(careUrl + "/internal/conversations", ApiModels.Conversation[].class);
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiModels.Conversation> createConversation(@RequestBody ApiModels.ConversationInput input) {
        return ResponseEntity.status(201).body(post(careUrl + "/internal/conversations", input, ApiModels.Conversation.class));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiModels.Message[] messages(@PathVariable long id) {
        return get(careUrl + "/internal/conversations/" + id + "/messages", ApiModels.Message[].class);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiModels.MessagePair> sendMessage(
            @PathVariable long id,
            @RequestBody ApiModels.MessageInput input
    ) {
        return ResponseEntity.status(201).body(post(careUrl + "/internal/conversations/" + id + "/messages", input, ApiModels.MessagePair.class));
    }

    private <T> T get(String url, Class<T> type) {
        return client.get().uri(url).retrieve().body(type);
    }

    private <T> T post(String url, Object body, Class<T> type) {
        return client.post().uri(url).body(body).retrieve().body(type);
    }
}