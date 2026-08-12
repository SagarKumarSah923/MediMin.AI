package com.medimin.care;

import com.medimin.common.ApiModels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
class AgentClient {
    private final RestClient client;
    private final String agentUrl;

    AgentClient(RestClient.Builder builder, @Value("${medimin.agent-service-url}") String agentUrl) {
        this.client = builder.build();
        this.agentUrl = agentUrl;
    }

    ApiModels.AgentChatResponse chat(List<ApiModels.AgentMessage> messages) {
        return client.post()
                .uri(agentUrl + "/internal/agent/chat")
                .body(new ApiModels.AgentChatRequest(messages))
                .retrieve()
                .body(ApiModels.AgentChatResponse.class);
    }

    ApiModels.AgentSymptomResponse analyze(ApiModels.SymptomCheckInput input) {
        return client.post()
                .uri(agentUrl + "/internal/agent/symptoms")
                .body(new ApiModels.AgentSymptomRequest(
                        input.symptoms(), input.duration(), input.severity(), input.notes()
                ))
                .retrieve()
                .body(ApiModels.AgentSymptomResponse.class);
    }
}