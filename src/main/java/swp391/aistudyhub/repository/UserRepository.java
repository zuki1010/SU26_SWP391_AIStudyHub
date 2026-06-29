package swp391.aistudyhub.repository;

import jakarta.websocket.server.PathParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN u.customerProfile cp " +
            "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :key, '%')) " +
            "OR LOWER(cp.fullName) LIKE LOWER(CONCAT('%', :key, '%'))")
    Page<UserAccountResponse> searchCustomers(@Param("key") String key, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    @Transactional
    int updateUserStatus(@PathParam("id") UUID id,@PathParam(("status")) AccountStatus status);

    Optional<User> findUserById(UUID id);

    Page<UserAccountResponse> findBy(Pageable pageable);
}
