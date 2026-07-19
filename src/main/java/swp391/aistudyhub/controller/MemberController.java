package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/member/")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Member Dashboard", description = "Subcription")
public class MemberController {
}
