package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//will process the response coming from AI
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {
    //to call AI through geminiService
    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        String aiRespose = geminiService.getRecommendations(prompt);
        log.info("RESPONSE FROM AI {} ", aiRespose);
        return processAIResponse(activity, aiRespose);
    }

    private Recommendation processAIResponse(Activity activity, String aiRespose) {
        try {
            //used to work with json(java obj<-->POJO)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiRespose); //for navigation inside json tree & we are at root
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .get("parts")
                    .get(0)
                    .path("text");
            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("\\n```", "")
                    .trim();
            //log.info("RESONSE FROM CLEANED AI {}", jsonContent);
            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");

            StringBuilder fullAnalysis = new StringBuilder();

            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall:");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace:");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate:");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories:");

            List<String> improvement = extractImprovement(analysisJson.path("improvements"));
        } catch (Exception e) {

        }
        return null;
    }

    private List<String> extractImprovement(JsonNode improvementsNode) {
        List<String> improvemnts = new ArrayList<>();
        if(improvementsNode.isArray()) {
            improvementsNode.forEach(improvemnt -> {
                String area = improvemnt.path("area").asText();
                String recommendation = improvemnt.path("recommendation").asText();
                String detail = improvemnt.path("recommendation").asText();
                improvemnts.add(String.format("%s: %s", area));
            });
        }
    }

    //"overall": "This was good" -->AI model response
    //Overall: This was good --> What we want to see
    //with this method it is converted, all things are added to StringBuilder
    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
        Analyze this fitness activity and provide detailed recommendations in the following EXACT JSON format:
        {
          "analysis": {
            "overall": "Overall analysis here",
            "pace": "Pace analysis here",
            "heartRate": "Heart rate analysis here",
            "caloriesBurned": "Calories analysis here"
          },
          "improvements": [
            {
              "area": "Area name",
              "recommendation": "Detailed recommendation"
            }
          ],
          "suggestions": [
            {
              "workout": "Workout name",
              "description": "Detailed workout description"
            }
          ],
          "safety": [
            "Safety point 1",
            "Safety point 2"
          ]
        }

        Analyze this activity:
        Activity Type: %s
        Duration: %d minutes
        Calories Burned: %d
        Additional Metrics: %s
        
        Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
        Ensure the response follows the EXACT JSON format shown above.
        """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }
}
