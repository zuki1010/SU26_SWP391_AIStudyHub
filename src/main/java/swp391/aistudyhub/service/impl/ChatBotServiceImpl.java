package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.ChatMessageRepository;
import swp391.aistudyhub.repository.ChatSessionRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ChatBotService;

import java.time.Instant;
import java.util.*;

@Service
public class ChatBotServiceImpl implements ChatBotService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public UUID createNewChatSession(StartSessionDTO dto) {
        UUID mockUserId = UUID.fromString("7b29a244-c782-4422-95cd-6562211f32a4");
        Optional<User> userOptional = userRepository.findById(mockUserId);
        if(userOptional.isEmpty()) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userOptional.get();

        ChatSession newSession = new ChatSession();
        newSession.setUser(user);
        newSession.setCreatedAt(Instant.now());

        if(dto.getDocumentId()!=null) {
            Optional<Document> documentOptional = documentRepository.findById(dto.getDocumentId());
            if(documentOptional.isEmpty()) {
                throw new RuntimeException("You don't have this document!");
            }
            Document document = documentOptional.get();
            newSession.setDocument(document);
            newSession.setSessionTitle("Chat about document: " + document.getDocumentName());
        } else {
            newSession.setDocument(null);
            newSession.setSessionTitle("New chat session");
        }

        ChatSession saveSession = chatSessionRepository.save(newSession);
        return saveSession.getId();
    }

    @Override
    public String chatWithNoDocument(ChatRequestSessionDTO dto) {
        ChatSession session = chatSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new RuntimeException("This chat session is not exist!"));


        List<ChatMessage> chatHistory = chatMessageRepository.findTop10ByChatSessionOrderBySentAtDesc(session);

        Collections.reverse(chatHistory);

        String aiAnswer = "This is the answer from AI";

        ChatMessage userMsg = new ChatMessage();
        userMsg.setChatSession(session);
        userMsg.setMessageContent(dto.getMessageContent());
        userMsg.setSenderType("USER");
        userMsg.setSentAt(Instant.now());
        chatMessageRepository.save(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setChatSession(session);
        aiMsg.setMessageContent(aiAnswer);
        aiMsg.setSenderType("BOT");
        aiMsg.setSentAt(Instant.now());
        chatMessageRepository.save(aiMsg);

        return aiAnswer;
    }

    public String chatTest(String message) {

        String apiKey = "...";
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", message)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map> candidates = (List<Map>) response.getBody().get("candidates");
                Map contentMap = (Map) candidates.get(0).get("content");
                List<Map> parts = (List<Map>) contentMap.get("parts");

                return (String) parts.get(0).get("text");
            }
        } catch (Exception e) {
            return "Lỗi kết nối API Gemini: " + e.getMessage();
        }

        return "Không nhận được phản hồi từ AI";
    }
}
