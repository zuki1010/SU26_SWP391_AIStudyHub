package swp391.aistudyhub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import swp391.aistudyhub.dto.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.StartSessionDTO;
import swp391.aistudyhub.service.ChatBotService;

import java.util.UUID;

@Controller
@RequestMapping("/api/chat")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @PostMapping
    public ResponseEntity<UUID> startChat(@RequestBody StartSessionDTO dto) {
        UUID newSessionId = chatBotService.createNewChatSession(dto);
        return ResponseEntity.ok(newSessionId);
    }

    public ResponseEntity<String> sendMessage(@RequestBody ChatRequestSessionDTO dto) {
        String aiAnswer = chatBotService.chatWithNoDocument(dto);

        return ResponseEntity.ok(aiAnswer);
    }
}
