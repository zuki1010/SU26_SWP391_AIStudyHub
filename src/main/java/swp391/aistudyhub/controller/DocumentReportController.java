package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CreateReportDTO;
import swp391.aistudyhub.dto.request.ProcessReportDTO;
import swp391.aistudyhub.dto.response.DocumentReportResponseDTO;
import swp391.aistudyhub.dto.response.ReportReasonResponseDTO;
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
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentReportController {

    private final DocumentReportServiceImpl reportService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<String> reportDocument(
            @RequestParam UUID documentId,
            @RequestParam ReportReason reason,
            @RequestParam(required = false) String description) {

        CreateReportDTO dto = new CreateReportDTO();
        dto.setDocumentId(documentId);
        dto.setReason(reason);
        dto.setDescription(description);

        String response = reportService.createReport(dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách report PENDING (Dành cho Admin/Moderator)")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<List<DocumentReportResponseDTO>> getPendingReports() {
        return ResponseEntity.ok(reportService.getAllPendingReports());
    }

    @PutMapping("/{reportId}/process")
    @PreAuthorize("hasAnyAuthority('MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<String> processReport(
            @PathVariable UUID reportId,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) String adminNote) {

        if (status == ReportStatus.PENDING) {
            throw new IllegalArgumentException("Trạng thái xử lý chỉ có thể là RESOLVED hoặc REJECTED!");
        }

        ProcessReportDTO dto = new ProcessReportDTO();
        dto.setStatus(status);
        dto.setAdminNote(adminNote);

        String response = reportService.processReport(reportId, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reasons")
    public ResponseEntity<List<ReportReasonResponseDTO>> getReportReasons() {
        List<ReportReasonResponseDTO> reasons = Arrays.stream(ReportReason.values())
                .map(reason -> new ReportReasonResponseDTO(reason, reason.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(reasons);
    }
}