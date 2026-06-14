package swp391.aistudyhub.component;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.entity.ChatMessage;

import java.util.List;

@Component
public class GeminiClient {
    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=YOUR_API_KEY";

    public String callGemini(String systemPrompt, List<ChatMessage> history, String userContent) {
        RestTemplate restTemplate = new RestTemplate();

        // Logic đóng gói JSON theo cấu trúc của Google Gemini (contents, parts, text...)
        // Bắn API bằng restTemplate.postForEntity(...)
        // Bóc tách JSON trả về chuỗi String câu trả lời của AI

        return "Câu trả lời từ cấu trúc cấu hình bên trong class này";
    }
}
