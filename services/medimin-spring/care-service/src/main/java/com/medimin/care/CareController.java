package com.medimin.care;

import com.medimin.common.ApiModels;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/internal")
public class CareController {
    private final AssessmentRepository assessments;
    private final SymptomCheckRepository symptomChecks;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AgentClient agent;
    private final RestClient client;
    private final String profileServiceUrl;

    public CareController(
            AssessmentRepository assessments,
            SymptomCheckRepository symptomChecks,
            ConversationRepository conversations,
            MessageRepository messages,
            AgentClient agent,
            RestClient.Builder builder,
            @Value("${medimin.profile-service-url}") String profileServiceUrl
    ) {
        this.assessments = assessments;
        this.symptomChecks = symptomChecks;
        this.conversations = conversations;
        this.messages = messages;
        this.agent = agent;
        this.client = builder.build();
        this.profileServiceUrl = profileServiceUrl;
    }

    @PostConstruct
    void seedCareData() {
        try {
            if (assessments.count() == 0) assessments.save(demoAssessment());
            if (conversations.count() == 0) {
                Instant now = Instant.now();
                conversations.save(demoConversation(now));
                messages.save(demoUserMessage(now));
                messages.save(demoAssistantMessage(now));
            }
        } catch (RuntimeException ignored) {
            // Atlas connectivity is reported by the API health path; keep demo mode available.
        }
    }

    @GetMapping("/assessments")
    public List<ApiModels.Assessment> listAssessments() {
        try {
            return assessments.findAllByOrderByCreatedAtDesc().stream().map(this::assessment).toList();
        } catch (RuntimeException ignored) {
            return List.of(assessment(demoAssessment()));
        }
    }

    @PostMapping("/assessments")
    public ResponseEntity<ApiModels.Assessment> createAssessment(@RequestBody ApiModels.AssessmentInput input) {
        Instant now = Instant.now();
        CareDocuments.Assessment saved = assessments.save(new CareDocuments.Assessment(
                nextId(assessments.findAll()), input.title(), "complete",
                "Your answers are a useful snapshot. Notice what changes over the next week and bring persistent concerns to a qualified clinician.",
                78, now, now, input.answers() == null ? Map.of() : input.answers()
        ));
        return ResponseEntity.status(201).body(assessment(saved));
    }

    @GetMapping("/assessments/{id}")
    public ApiModels.Assessment getAssessment(@PathVariable long id) {
        try {
            return assessments.findById(id).map(this::assessment).orElseGet(() -> assessment(demoAssessment()));
        } catch (RuntimeException ignored) {
            return assessment(demoAssessment());
        }
    }

    @PostMapping("/symptom-checks")
    public ResponseEntity<ApiModels.SymptomCheck> createSymptomCheck(@RequestBody ApiModels.SymptomCheckInput input) {
        ApiModels.AgentSymptomResponse result = agent.analyze(input);
        CareDocuments.SymptomCheck saved = symptomChecks.save(new CareDocuments.SymptomCheck(
                nextId(symptomChecks.findAll()), input.symptoms(), input.duration(), input.severity(),
                input.notes() == null ? "" : input.notes(), result.possibleCauses(), result.guidance(),
                result.urgency(), result.disclaimer(), Instant.now()
        ));
        return ResponseEntity.status(201).body(symptomCheck(saved));
    }

    @GetMapping("/conversations")
    public List<ApiModels.Conversation> listConversations() {
        try {
            return conversations.findAllByOrderByUpdatedAtDesc().stream().map(this::conversation).toList();
        } catch (RuntimeException ignored) {
            return List.of(conversation(demoConversation(Instant.now())));
        }
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiModels.Conversation> createConversation(@RequestBody ApiModels.ConversationInput input) {
        Instant now = Instant.now();
        CareDocuments.Conversation saved = conversations.save(new CareDocuments.Conversation(
                nextId(conversations.findAll()), input.title(), "A new conversation with MediMin AI", 0, now
        ));
        return ResponseEntity.status(201).body(conversation(saved));
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ApiModels.Message> listMessages(@PathVariable long id) {
        try {
            return messages.findByConversationIdOrderByCreatedAtAsc(id).stream().map(this::message).toList();
        } catch (RuntimeException ignored) {
            return List.of(message(demoUserMessage(Instant.now().minusSeconds(60))), message(demoAssistantMessage(Instant.now())));
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiModels.MessagePair> sendMessage(
            @PathVariable long id,
            @RequestBody ApiModels.MessageInput input
    ) {
        CareDocuments.Conversation conversation = conversations.findById(id).orElseThrow();
        Instant userTime = Instant.now();
        CareDocuments.Message user = messages.save(new CareDocuments.Message(
                nextId(messages.findAll()), id, "user", input.content(), userTime
        ));
        List<ApiModels.AgentMessage> history = messages.findByConversationIdOrderByCreatedAtAsc(id)
                .stream()
                .map(item -> new ApiModels.AgentMessage(item.role(), item.content()))
                .toList();
        ApiModels.AgentChatResponse answer = agent.chat(history);
        CareDocuments.Message assistant = messages.save(new CareDocuments.Message(
                nextId(messages.findAll()), id, "assistant", answer.content(), Instant.now()
        ));
        conversations.save(new CareDocuments.Conversation(
                conversation.id(), conversation.title(), answer.content(), conversation.messageCount() + 2, Instant.now()
        ));
        return ResponseEntity.status(201).body(new ApiModels.MessagePair(message(user), message(assistant)));
    }

    @GetMapping("/dashboard")
    public ApiModels.Dashboard dashboard() {
        ApiModels.Profile profile = client.get().uri(profileServiceUrl + "/internal/profile")
                .retrieve().body(ApiModels.Profile.class);
        List<CareDocuments.Assessment> allAssessments;
        try {
            allAssessments = assessments.findAllByOrderByCreatedAtDesc();
        } catch (RuntimeException ignored) {
            allAssessments = List.of(demoAssessment());
        }
        CareDocuments.Assessment latest = allAssessments.isEmpty() ? null : allAssessments.get(0);
        List<CareDocuments.Conversation> allConversations;
        try {
            allConversations = conversations.findAllByOrderByUpdatedAtDesc();
        } catch (RuntimeException ignored) {
            allConversations = List.of(demoConversation(Instant.now()));
        }
        List<ApiModels.Activity> activity = new ArrayList<>();
        if (latest != null) {
            activity.add(new ApiModels.Activity(latest.id(), "assessment", "Completed a health check-in", latest.title(), latest.createdAt()));
        }
        if (!allConversations.isEmpty()) {
            CareDocuments.Conversation chat = allConversations.get(0);
            activity.add(new ApiModels.Activity(10000 + chat.id(), "conversation", "Talked with MediMin AI", chat.title(), chat.updatedAt()));
        }
        int score = latest == null ? 0 : latest.score();
        List<ApiModels.Metric> metrics = List.of(
                new ApiModels.Metric("Check-ins completed", String.valueOf(allAssessments.size()), "Keep noticing the patterns", "teal"),
                new ApiModels.Metric("Last check-in", latest == null ? "—" : latest.score() + "/100", "A reflection, not a diagnosis", "sun")
        );
        return new ApiModels.Dashboard(
                profile, metrics, latest == null ? null : assessment(latest), activity, score,
                score == 0 ? "Building your picture" : "A useful snapshot"
        );
    }

    private static long nextId(List<?> records) {
        return records.size() + 1L;
    }

    private CareDocuments.Assessment demoAssessment() {
        Instant now = Instant.now();
        return new CareDocuments.Assessment(
                1L, "Energy & sleep check-in", "complete",
                "Your answers suggest a useful baseline: notice how rest, routine, and energy move together over the next week.",
                78, now, now,
                Map.of("focus", "Feeling more tired in the afternoons", "sleep", "Around 7 hours", "mood", "Mostly steady")
        );
    }

    private CareDocuments.Conversation demoConversation(Instant now) {
        return new CareDocuments.Conversation(
                1L, "Feeling tired lately",
                "I can help you look at timing, sleep, and possible next questions.", 2, now
        );
    }

    private CareDocuments.Message demoUserMessage(Instant time) {
        return new CareDocuments.Message(
                1L, 1L, "user",
                "I have been feeling tired in the afternoons. What should I pay attention to?", time
        );
    }

    private CareDocuments.Message demoAssistantMessage(Instant time) {
        return new CareDocuments.Message(
                2L, 1L, "assistant",
                "A good place to start is to notice whether the pattern follows your sleep, meals, hydration, stress, or activity. Keep a short note for a few days, and speak with a clinician if the fatigue is persistent, worsening, or affecting daily life.",
                time
        );
    }

    private ApiModels.Assessment assessment(CareDocuments.Assessment item) {
        return new ApiModels.Assessment(item.id(), item.title(), item.status(), item.summary(), item.score(), item.createdAt(), item.updatedAt(), item.answers());
    }

    private ApiModels.SymptomCheck symptomCheck(CareDocuments.SymptomCheck item) {
        return new ApiModels.SymptomCheck(item.id(), item.symptoms(), item.duration(), item.severity(), item.notes(), item.possibleCauses(), item.guidance(), item.urgency(), item.disclaimer(), item.createdAt());
    }

    private ApiModels.Conversation conversation(CareDocuments.Conversation item) {
        return new ApiModels.Conversation(item.id(), item.title(), item.preview(), item.messageCount(), item.updatedAt());
    }

    private ApiModels.Message message(CareDocuments.Message item) {
        return new ApiModels.Message(item.id(), item.conversationId(), item.role(), item.content(), item.createdAt());
    }
}