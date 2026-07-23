package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.dto.projection.SystemConfigResponse;
import swp391.aistudyhub.entity.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    SystemConfigResponse findBy();
}