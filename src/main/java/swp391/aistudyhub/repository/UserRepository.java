package swp391.aistudyhub.repository;

import jakarta.websocket.server.PathParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<UserAccountResponseDTO> findByEmailContainingIgnoreCaseOrCustomerProfileFullNameContainingIgnoreCase(
            String emailKeyword,
            String fullNameKeyword,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    @Transactional
    int updateUserStatus(@PathParam("id") UUID id,@PathParam(("status")) AccountStatus status);

    Optional<User> findUserById(UUID id);
}
