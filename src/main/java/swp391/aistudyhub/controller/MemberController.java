package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.response.PaymentResponseDTO;
import swp391.aistudyhub.service.MemberService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@Tag(name = "Member Dashboard", description = "Subscription")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/payment/create")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<PaymentResponseDTO> createPremiumPayment() {
        return ResponseEntity.ok(memberService.createPremiumPayment());
    }

    @PostMapping("/payment/confirm")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<PaymentResponseDTO> confirmPremiumPayment(
            @RequestParam("orderCode") Long orderCode
    ) {
        return ResponseEntity.ok(memberService.confirmPremiumPayment(orderCode));
    }

    @PostMapping("/payment/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<?> cancelPremiumPayment(
            @RequestParam("orderCode") Long orderCode
    ) {
        memberService.cancelPremiumPayment(orderCode);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/payment/history")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentHistory() {
        return ResponseEntity.ok(memberService.getPaymentHistory());
    }

    @PostMapping("/payment/webhook")
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        memberService.handlePaymentWebhook(payload);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<?> registerMember() {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "success", false,
                        "message", "Vui lòng xác nhận giao dịch trước khi kích hoạt Premium."
                )
        );
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getDetail() {
        return ResponseEntity.ok(memberService.getMemberDetail());
    }
}