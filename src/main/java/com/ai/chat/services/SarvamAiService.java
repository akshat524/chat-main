package com.ai.chat.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ai.chat.models.ChatMessage;

@Service
public class SarvamAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askSarvam(List<ChatMessage> history, String userMessage) {

        String url = "https://api.sarvam.ai/v1/chat/completions";

        // Build messages list
        List<Map<String, String>> messages = new ArrayList<>();

        // System message
        messages.add(Map.of(
                "role", "system",
                "content", "You are a helpful AI assistant."
        ));

        // Previous chat history
        for (ChatMessage msg : history) {
            messages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
            ));
        }

        // Current user message
        messages.add(Map.of(
                "role", "user",
                "content", userMessage
        ));

        // Request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 1000);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        try {

            // API call
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    entity,
                    Map.class
            );

            // Full response logging
            Map responseBody = response.getBody();

            System.out.println("========== SARVAM RESPONSE ==========");
            System.out.println(responseBody);
            System.out.println("=====================================");

            // Null check
            if (responseBody == null) {
                return "Error: Empty response from AI service.";
            }

            // Extract choices
            Object choicesObj = responseBody.get("choices");

            if (choicesObj == null) {
                return "Error: 'choices' not found in AI response.";
            }

            List choices = (List) choicesObj;

            if (choices.isEmpty()) {
                return "Error: AI returned empty choices.";
            }

            // First choice
            Map firstChoice = (Map) choices.get(0);

            // Extract message
            Object messageObj = firstChoice.get("message");

            if (messageObj == null) {
                return "Error: 'message' not found in AI response.";
            }

            Map messageMap = (Map) messageObj;

            // Extract content
            Object contentObj = messageMap.get("content");

            if (contentObj == null) {
                return "Error: 'content' not found in AI response.";
            }

            // If content is direct string
            if (contentObj instanceof String) {
                return contentObj.toString();
            }

            // If content is list/array
            if (contentObj instanceof List) {

                List contentList = (List) contentObj;

                if (contentList.isEmpty()) {
                    return "Error: Empty content list.";
                }

                Object firstItemObj = contentList.get(0);

                if (firstItemObj instanceof Map) {

                    Map firstItem = (Map) firstItemObj;

                    Object textObj = firstItem.get("text");

                    if (textObj != null) {
                        return textObj.toString();
                    }
                }
            }

            return "Error: Unable to parse AI response.";

        } catch (Exception e) {

            System.out.println("========== SARVAM ERROR ==========");
            e.printStackTrace();
            System.out.println("==================================");

            return "Error communicating with AI service.";
        }
    }
}