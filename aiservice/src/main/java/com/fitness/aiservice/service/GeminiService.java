package com.fitness.aiservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
//@RequiredArgsConstructor //it removed the red underline of Webclient
public class GeminiService {

    //with this instance we will call the gemini api, give the activity data
    //then will parse the response and save it in db
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClinetBuilder) {
        this.webClient = WebClient.builder().build();
    }

    public String getRecommendations(String details) {
        Map<String, Object> requestBody = Map.of(
               "contents", new Object[] {
                       Map.of("parts", new Object[]{
                               Map.of("text", details)
                       })
                }
        );

        String response = webClient.post()
                .uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return response;
    }
}
