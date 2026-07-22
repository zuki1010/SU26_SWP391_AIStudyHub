package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.DocumentReport;
import swp391.aistudyhub.enums.ReportStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentReportRepository extends JpaRepository<DocumentReport, UUID> {

    // Kiểm tra xem User đã từng report bài doc này và đang chờ duyệt chưa (tránh spam report)
    boolean existsByDocument_IdAndReporter_IdAndStatus(UUID documentId, UUID reporterId, ReportStatus status);

    // Tìm kiếm danh sách report theo trạng thái cho Admin/Mod
    Page<DocumentReport> findByStatus(ReportStatus status, Pageable pageable);

    List<DocumentReport> findByStatus(ReportStatus status);
}