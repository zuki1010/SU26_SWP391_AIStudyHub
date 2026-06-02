package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.dto.StartSessionDTO;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.ChatSessionRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ChatBotService;

import java.time.Instant;
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
}
