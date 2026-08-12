package com.medimin.care;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class CareDocuments {
    private CareDocuments() {}

    @Document("assessments")
    record Assessment(
            @Id Long id,
            String title,
            String status,
            String summary,
            int score,
            Instant createdAt,
            Instant updatedAt,
            Map<String, String> answers
    ) {}

    @Document("symptom_checks")
    record SymptomCheck(
            @Id Long id,
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

    @Document("conversations")
    record Conversation(
            @Id Long id,
            String title,
            String preview,
            int messageCount,
            Instant updatedAt
    ) {}

    @Document("messages")
    record Message(
            @Id Long id,
            long conversationId,
            String role,
            String content,
            Instant createdAt
    ) {}
}