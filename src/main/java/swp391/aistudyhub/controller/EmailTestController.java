package swp391.aistudyhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.service.EmailService;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/test")
    public ResponseEntity<?> sendTestEmail(@RequestParam String to) {
        emailService.sendTestEmail(to);

        return ResponseEntity.ok("Test email sent successfully.");
    }
}