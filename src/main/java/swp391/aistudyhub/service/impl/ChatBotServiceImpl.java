package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
