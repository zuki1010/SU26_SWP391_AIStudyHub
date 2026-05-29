package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}