package swp391.aistudyhub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.service.ChatBotService;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @PostMapping("/start")
    public ResponseEntity<UUID> startChat(@RequestBody StartSessionDTO dto) {
        UUID newSessionId = chatBotService.createNewChatSession(dto);
        return ResponseEntity.ok(newSessionId);
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatRequestSessionDTO dto) {
        String aiAnswer = chatBotService.chatWithNoDocument(dto);

        return ResponseEntity.ok(aiAnswer);
    }
}
