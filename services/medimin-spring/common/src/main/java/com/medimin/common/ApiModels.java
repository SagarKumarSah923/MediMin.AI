package com.medimin.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {}

    public record HealthStatus(String status) {}

    public record ErrorResponse(String error) {}

    public record Profile(
            long id,
            String firstName,
            String lastName,
            String email,
            String dateOfBirth,
            String bloodType,
            List<String> allergies,
            List<String> conditions,
            int medicationCount
    ) {}

    public record Metric(String label, String value, String detail, String tone) {}

    public record Activity(long id, String kind, String title, String detail, Instant occurredAt) {}

    public record Assessment(
            long id,
            String title,
            String status,
            String summary,
            int score,
            Instant createdAt,
            Instant updatedAt,
            Map<String, String> answers
    ) {}

    public record AssessmentInput(String title, Map<String, String> answers) {}

    public record Dashboard(
            Profile profile,
            List<Metric> metrics,
            Assessment latestAssessment,
            List<Activity> recentActivity,
            int healthScore,
            String healthScoreLabel
    ) {}

    public record SymptomCheckInput(
            List<String> symptoms,
            String duration,
            String severity,
            String notes
    ) {}

    public record SymptomCheck(
            long id,
            List<String> symptoms,
            String duration,
            String severity,
            String notes,
            List<String> possibleCauses,
            List<String> guidance,
            String urgency,
            String disclaimer,
            Instant createdAt
    ) {}

    public record Conversation(long id, String title, String preview, int messageCount, Instant updatedAt) {}

    public record ConversationInput(String title) {}

    public record Message(long id, long conversationId, String role, String content, Instant createdAt) {}

    public record MessageInput(String content) {}

    public record MessagePair(Message userMessage, Message assistantMessage) {}

    public record AgentChatRequest(List<AgentMessage> messages) {}

    public record AgentMessage(String role, String content) {}

    public record AgentChatResponse(String content) {}

    public record AgentSymptomRequest(
            List<String> symptoms,
            String duration,
            String severity,
            String notes
    ) {}

    public record AgentSymptomResponse(
            List<String> possibleCauses,
            List<String> guidance,
            String urgency,
            String disclaimer
    ) {}
}