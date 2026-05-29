package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.CustomerProfile;

import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
}