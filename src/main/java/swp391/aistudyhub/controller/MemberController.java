package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.service.MemberService;

@RestController("/api/member/")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@Tag(name = "Member Dashboard", description = "Subscription")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<?> registerMember() {
        memberService.registerMember();
        return ResponseEntity.ok().body("Register Successfully! You are now a member of our system");
    }
}
