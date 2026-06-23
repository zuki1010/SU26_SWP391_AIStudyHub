package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumReport;
import swp391.aistudyhub.enums.ReportStatus;

import java.util.UUID;

@Repository
public interface ForumReportRepository extends JpaRepository<ForumReport, UUID> {

    Page<ForumReport> findByStatus(ReportStatus status, Pageable pageable);

    boolean existsByReporter_IdAndTargetIdAndStatus(UUID reporterId, UUID targetId, ReportStatus status);
}
