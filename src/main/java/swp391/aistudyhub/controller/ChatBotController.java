package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
//import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.ChatMessageDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.service.ChatBotService;
import swp391.aistudyhub.service.DocumentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI ChatBot", description = "Create session, chat")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @Autowired
    private DocumentService documentService;

    @PostMapping("/start")
    public ResponseEntity<?> startChat(@RequestBody(required = false) StartSessionDTO dto) {
        UUID sessionId = chatBotService.createNewChatSession(dto);
        return ResponseEntity.ok().body("Chat Session Created. Session ID: " + sessionId);
    }

    @PutMapping("/session/{sessionId}/documents")
    public ResponseEntity<?> updateDocuments(
            @PathVariable UUID sessionId,
            @RequestBody UpdateSessionDocsDTO dto) {
        chatBotService.updateSessionDocuments(sessionId, dto);
        return ResponseEntity.ok().body("Update Documents List Successfully!");
    }

    @PostMapping("/send-message")
    public ResponseEntity<String> sendMessage(@RequestBody ChatRequestSessionDTO dto) {
        if (dto.getSessionId() == null || dto.getMessageContent() == null || dto.getMessageContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("This field cannot be empty!");
        }

        try {
            String aiResponse = chatBotService.chatWithGemini(dto);

            return ResponseEntity.ok().body(aiResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Chat Process Error: " + e.getMessage());
        }
    }
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ChatMessageDTO> history = chatBotService.getChatHistory(sessionId, page, size);
        return ResponseEntity.ok(history);
    }
}
