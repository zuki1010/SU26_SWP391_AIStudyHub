package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;
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
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }

        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        ChatSession newSession = new ChatSession();
        newSession.setUser(user);
        newSession.setCreatedAt(Instant.now());

        if (dto != null && dto.getDocumentIds() != null && !dto.getDocumentIds().isEmpty()) {

            List<Document> documents = documentRepository.findAllById(dto.getDocumentIds());
            if (documents.isEmpty()) {
                throw new RuntimeException("Documents are not found!");
            }

            newSession.getDocuments().addAll(documents);

            if (documents.size() == 1) {
                newSession.setSessionTitle("Chat about documents: " + documents.get(0).getDocumentName());
            } else {
                newSession.setSessionTitle("Chat about " + documents.size() + " documents selected");
            }

        } else {
            newSession.setSessionTitle("New chat session");
        }

        ChatSession savedSession = chatSessionRepository.save(newSession);
        return savedSession.getId();
    }

    @Override
    public void updateSessionDocuments(UUID sessionId, UpdateSessionDocsDTO dto) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("This chat session is not found!"));

        session.getDocuments().clear();

        if (dto != null && dto.getDocumentIds() != null && !dto.getDocumentIds().isEmpty()) {
            List<Document> targetDocuments = documentRepository.findAllById(dto.getDocumentIds());

            session.getDocuments().addAll(targetDocuments);
            session.setSessionTitle("Chat about " + targetDocuments.size() + " documents selected");
        } else {
            session.setSessionTitle("Chat Free Session");
        }

        chatSessionRepository.save(session);
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
