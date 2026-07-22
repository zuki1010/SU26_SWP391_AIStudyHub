package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreateReportDTO;
import swp391.aistudyhub.dto.request.ProcessReportDTO;
import swp391.aistudyhub.dto.response.DocumentReportResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.DocumentReport;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.ReportReason;
import swp391.aistudyhub.enums.ReportStatus;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.DocumentReportRepository;
import swp391.aistudyhub.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentReportServiceImpl {

    private final DocumentReportRepository reportRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository; // 👈 Inject UserRepository

    /**
     * Helper: Tự bóc tách Email từ SecurityContextHolder và tìm User trong DB
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại trong hệ thống!"));
    }

    /**
     * 1. User tạo Báo cáo (Report) cho Bài Doc Public
     */
    @Transactional
    public String createReport(CreateReportDTO dto) { // 👈 Bỏ tham số User currentUser
        User currentUser = getCurrentUser(); // 👈 Lấy trực tiếp tại đây

        // 🔴 Validate 1: Nếu chọn lý do "OTHER", bắt buộc phải nhập mô tả
        if (dto.getReason() == ReportReason.OTHER &&
                (dto.getDescription() == null || dto.getDescription().trim().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng nhập chi tiết mô tả vấn đề khi chọn 'Lý do khác'!");
        }

        Document document = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu!"));

        // 🔴 Validate 2: Chỉ cho phép report tài liệu đang ở chế độ Public
        if (!document.isPublic()) {
            throw new IllegalArgumentException("Chỉ có thể báo cáo tài liệu ở chế độ công khai (Public)!");
        }

        // 🔴 Validate 3: Không cho phép tự report tài liệu của chính mình
        if (document.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể báo cáo tài liệu do chính mình đăng!");
        }

        // 🔴 Validate 4: Tránh 1 user gửi liên tục nhiều report PENDING cho cùng 1 file
        boolean alreadyReported = reportRepository.existsByDocument_IdAndReporter_IdAndStatus(
                document.getId(), currentUser.getId(), ReportStatus.PENDING
        );
        if (alreadyReported) {
            throw new IllegalArgumentException("Bạn đã báo cáo tài liệu này trước đó và đang chờ Mod/Admin xử lý.");
        }

        // Tạo đối tượng Report
        DocumentReport report = DocumentReport.builder()
                .document(document)
                .reporter(currentUser)
                .reason(dto.getReason())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .status(ReportStatus.PENDING)
                .build();

        reportRepository.save(report);
        return "Báo cáo của bạn đã được gửi thành công. Ban quản trị sẽ xem xét!";
    }

    /**
     * 2. Mod/Admin Lấy danh sách Report chờ xử lý
     */
    @Transactional(readOnly = true)
    public Page<DocumentReportResponseDTO> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(ReportStatus.PENDING, pageable)
                .map(this::mapToResponseDTO);
    }

    /**
     * 3. Mod/Admin Duyệt/Xử lý Report (RESOLVED hoặc REJECTED)
     */
    @Transactional
    public String processReport(UUID reportId, ProcessReportDTO dto) { // 👈 Bỏ tham số User adminUser
        if (dto.getStatus() == null || dto.getStatus() == ReportStatus.PENDING) {
            throw new IllegalArgumentException("Trạng thái xử lý phải là RESOLVED (Chấp nhận) hoặc REJECTED (Từ chối)!");
        }

        DocumentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo này!"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalArgumentException("Báo cáo này đã được xử lý trước đó!");
        }

        report.setStatus(dto.getStatus());
        report.setAdminNote(dto.getAdminNote());

        // Nếu Chấp nhận Report (RESOLVED) -> Tiến hành gỡ bài Doc Public
        if (dto.getStatus() == ReportStatus.RESOLVED) {
            Document doc = report.getDocument();

            doc.setPublic(false); // Gỡ khỏi danh sách Public
            if (doc.getStatus() != null) {
                doc.setStatus(StatusPublicDoc.DENY); // Đổi trạng thái hiển thị
            }

            documentRepository.save(doc);
        }

        reportRepository.save(report);
        return "Đã xử lý báo cáo thành công!";
    }

    /**
     * Helper Mapper: Chuyển đổi Entity sang Response DTO an toàn
     */
    private DocumentReportResponseDTO mapToResponseDTO(DocumentReport report) {
        String reporterName = "Khách";
        if (report.getReporter() != null) {
            reporterName = report.getReporter().getEmail();
        }

        return DocumentReportResponseDTO.builder()
                .id(report.getId())
                .documentId(report.getDocument() != null ? report.getDocument().getId() : null)
                .documentName(report.getDocument() != null ? report.getDocument().getDocumentName() : null)
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterName(reporterName)
                .reason(report.getReason())
                .reasonDescription(report.getReason() != null ? report.getReason().getDescription() : null)
                .description(report.getDescription())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentReportResponseDTO> getAllPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }
}