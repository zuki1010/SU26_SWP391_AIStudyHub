package swp391.aistudyhub.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.component.GeminiClient;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.ChatMessageDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.SenderType;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.ChatBotService;
import swp391.aistudyhub.service.DocumentChunkService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

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

        User user = session.getUser();

        UserMemberSubscription userMemberSubscription = userMemberSubscriptionRepository.findByUser(user)
                .orElse(null);
        CustomerProfile profile = null;
        boolean isCustomer = "CUSTOMER".equals(user.getRole().name());
        boolean isModerator = "MODERATOR".equals(user.getRole().name());

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));

        if (isCustomer || isModerator) {
            SystemConfig systemConfig = systemConfigRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("This config is not available"));
            int maxDailyTokens = 0;

            if(userMemberSubscription == null) {
                maxDailyTokens = systemConfig.getMaxDailyChatTokens();
            } else {
                maxDailyTokens = subscriptionPlan.getMaxDailyChatTokens();
            }

            profile = customerProfileRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Customer Profile not found"));

            java.time.LocalDate today = java.time.LocalDate.now();

            if (profile.getLast_chat_date() == null || !profile.getLast_chat_date().equals(today)) {
                profile.setTokens_used_today(0);
                profile.setLast_chat_date(today);
                profile = customerProfileRepository.save(profile);
            }

            if (profile.getTokens_used_today() >= maxDailyTokens) {
                throw new RuntimeException("Bạn đã dùng hết giới hạn token chat của ngày hôm nay!");
            }
        }

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
            systemPrompt += "Answer Questions base on documents:\n" + documentContext;
        }

        GeminiClient.GeminiResult geminiResult = geminiClient.callGemini(systemPrompt, history, dto.getMessageContent());
        String aiResponse = geminiResult.getTextResponse();
        int tokensSpentForThisTurn = geminiResult.getTotalTokens();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setChatSession(session);
        userMsg.setSenderType(SenderType.USER);
        userMsg.setMessageContent(dto.getMessageContent());
        userMsg.setSentAt(Instant.now());
        chatMessageRepository.save(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setChatSession(session);
        aiMsg.setSenderType(SenderType.AI);
        aiMsg.setMessageContent(aiResponse);
        aiMsg.setSentAt(Instant.now());
        chatMessageRepository.save(aiMsg);

        if (isCustomer && profile != null) {
            profile.setTokens_used_today(profile.getTokens_used_today() + tokensSpentForThisTurn);
            customerProfileRepository.save(profile);
        }

        return aiResponse;
    }

    @Override
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
                        msg.getSenderType().name(),
                        msg.getSentAt()
                ))
                .collect(Collectors.toList());

        List<ChatMessageDTO> history = new ArrayList<>(dtoList);
        Collections.reverse(history);

        return history;
    }
}
