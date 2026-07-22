package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.dto.request.CreateReportDTO;
import swp391.aistudyhub.dto.request.ProcessReportDTO;
import swp391.aistudyhub.dto.response.DocumentReportResponseDTO;
import swp391.aistudyhub.dto.response.ReportReasonResponseDTO;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.ReportReason;
import swp391.aistudyhub.enums.ReportStatus;
import swp391.aistudyhub.service.impl.DocumentReportServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class DocumentReportController {

    private final DocumentReportServiceImpl reportService;

    // Helper lấy Current User từ SecurityContextHolder
    private User getCurrentUser() {
        // ... Lấy user hiện tại đang đăng nhập
        return new User();
    }

    /**
     * 🔴 1. User báo cáo 1 bài doc Public
     *
     * Cách 1: Dùng @RequestParam để Swagger UI tạo sẵn Dropdown chọn Enum lý do
     */
    @PostMapping
    public ResponseEntity<String> reportDocument(
            @RequestParam UUID documentId,
            @RequestParam ReportReason reason, // 👈 Swagger sẽ tự hiện Dropdown danh sách Enum ở đây!
            @RequestParam(required = false) String description) {

        CreateReportDTO dto = new CreateReportDTO();
        dto.setDocumentId(documentId);
        dto.setReason(reason);
        dto.setDescription(description);

        User currentUser = getCurrentUser();
        String response = reportService.createReport(dto, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 🛡️ 2. Mod/Admin lấy danh sách report PENDING (Trả về Response DTO an toàn)
     */
    @Operation(summary = "Lấy danh sách report PENDING (Dành cho Admin/Moderator)")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<DocumentReportResponseDTO>> getPendingReports() {
        return ResponseEntity.ok(reportService.getAllPendingReports());
    }

    /**
     * 🛡️ 3. Mod/Admin duyệt report (Chấp nhận gỡ bài hoặc Từ chối report)
     */
    @PutMapping("/{reportId}/process")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<String> processReport(
            @PathVariable UUID reportId,
            @RequestParam ReportStatus status, // 👈 Swagger sẽ tự hiện Dropdown chọn RESOLVED / REJECTED
            @RequestParam(required = false) String adminNote) {

        if (status == ReportStatus.PENDING) {
            throw new IllegalArgumentException("Trạng thái xử lý chỉ có thể là RESOLVED (Chấp nhận) hoặc REJECTED (Từ chối)!");
        }

        User admin = getCurrentUser();

        // Tạo DTO truyền sang Service
        ProcessReportDTO dto = new ProcessReportDTO();
        dto.setStatus(status);
        dto.setAdminNote(adminNote);

        String response = reportService.processReport(reportId, dto, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * 📋 4. API Lấy danh sách các lý do báo cáo (Dành cho Frontend Web/App gọi đổ vào Dropdown UI)
     */
    @GetMapping("/reasons")
    public ResponseEntity<List<ReportReasonResponseDTO>> getReportReasons() {
        List<ReportReasonResponseDTO> reasons = Arrays.stream(ReportReason.values())
                .map(reason -> new ReportReasonResponseDTO(reason, reason.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(reasons);
    }
}