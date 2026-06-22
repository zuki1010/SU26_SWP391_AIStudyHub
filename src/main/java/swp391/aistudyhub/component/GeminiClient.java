package swp391.aistudyhub.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.enums.SenderType;

import java.util.*;

@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public String callGemini(String systemPrompt, List<ChatMessage> history, String userMessageContent) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            Map<String, Object> systemInstruction = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", systemPrompt);
            systemInstruction.put("parts", Collections.singletonList(parts));
            requestBody.put("systemInstruction", systemInstruction);
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            for (ChatMessage msg : history) {
                Map<String, Object> contentItem = new HashMap<>();
                String role = (msg.getSenderType() == SenderType.USER) ? "user" : "model";
                contentItem.put("role", role);

                Map<String, Object> partsItem = new HashMap<>();
                partsItem.put("text", msg.getMessageContent());
                contentItem.put("parts", Collections.singletonList(partsItem));

                contents.add(contentItem);
            }
        }

        Map<String, Object> currentUserContent = new HashMap<>();
        currentUserContent.put("role", "user");
        Map<String, Object> currentUserParts = new HashMap<>();
        currentUserParts.put("text", userMessageContent);
        currentUserContent.put("parts", Collections.singletonList(currentUserParts));
        contents.add(currentUserContent);

        requestBody.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) firstCandidate.get("content");

                    if (contentMap != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(">>> GEMINI API ERROR: " + e.getMessage());
            return "Lỗi kết nối API Gemini: " + e.getMessage();
        }

        return "Không nhận được phản hồi hợp lệ từ AI";
    }
}
