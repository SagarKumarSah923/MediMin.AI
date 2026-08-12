package com.medimin.care;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

interface AssessmentRepository extends MongoRepository<CareDocuments.Assessment, Long> {
    List<CareDocuments.Assessment> findAllByOrderByCreatedAtDesc();
}

interface SymptomCheckRepository extends MongoRepository<CareDocuments.SymptomCheck, Long> {}

interface ConversationRepository extends MongoRepository<CareDocuments.Conversation, Long> {
    List<CareDocuments.Conversation> findAllByOrderByUpdatedAtDesc();
}

interface MessageRepository extends MongoRepository<CareDocuments.Message, Long> {
    List<CareDocuments.Message> findByConversationIdOrderByCreatedAtAsc(long conversationId);
}