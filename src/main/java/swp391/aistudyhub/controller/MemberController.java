package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.service.MemberService;

import java.util.Map;

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR')")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@Tag(name = "Member Dashboard", description = "Subscription")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<?> registerMember() {
        memberService.registerMember();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Thanh toán thành công! Tài khoản đã được nâng cấp Premium.",
                        "plan", "PREMIUM",
                        "membership", "PREMIUM",
                        "isPremium", true
                )
        );
    }
}