package swp391.aistudyhub.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.component.GeminiClient;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.ChatMessageDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.ChatBotService;
import swp391.aistudyhub.service.DocumentChunkService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatBotServiceImpl implements ChatBotService {

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentChunkService documentChunkService;

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

    @Override
    @Transactional
    public String chatWithGemini(ChatRequestSessionDTO dto) {
        ChatSession session = chatSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new RuntimeException("This Chat Session is not found"));

        List<ChatMessage> history = chatMessageRepository.findTop10ByChatSessionOrderBySentAtDesc(session);
        Collections.reverse(history);

        Set<Document> attachedDocs = session.getDocuments();
        String documentContext = "";

        if (attachedDocs != null && !attachedDocs.isEmpty()) {
            List<UUID> docIds = attachedDocs.stream().map(Document::getId).toList();

            String embeddingResult = documentChunkService.getVectorStringForQuery(dto.getMessageContent());

            List<String> relevantChunks = documentChunkRepository.findRelevantChunks(docIds, embeddingResult, 5);

            documentContext = String.join("\n\n", relevantChunks);
        }

        String systemPrompt = "Bạn là trợ lý học tập. ";
        if (!documentContext.isEmpty()) {
            systemPrompt += "Answer Questions base on documents:\n"
                    + documentContext;
        }

        String aiResponse = geminiClient.callGemini(systemPrompt, history, dto.getMessageContent());

        ChatMessage userMsg = new ChatMessage();
        userMsg.setChatSession(session);
        userMsg.setSenderType("USER");
        userMsg.setMessageContent(dto.getMessageContent());
        chatMessageRepository.save(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setChatSession(session);
        aiMsg.setSenderType("AI");
        aiMsg.setMessageContent(aiResponse);
        chatMessageRepository.save(aiMsg);

        return aiResponse;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(UUID sessionId, int page, int size) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat Session không tồn tại"));

        Page<ChatMessage> messagePage = chatMessageRepository.findByChatSessionOrderBySentAtDesc(
                session,
                PageRequest.of(page, size)
        );

        List<ChatMessageDTO> dtoList = messagePage.getContent().stream()
                .map(msg -> new ChatMessageDTO(
                        msg.getId(),
                        msg.getMessageContent(),
                        msg.getSenderType(),
                        msg.getSentAt()
                ))
                .collect(Collectors.toList());

        List<ChatMessageDTO> history = new ArrayList<>(dtoList);
        Collections.reverse(history);

        return history;
    }
}
