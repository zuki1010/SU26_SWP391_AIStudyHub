package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findUserById(UUID id);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN u.customerProfile cp
            LEFT JOIN u.moderatorProfile mp
            LEFT JOIN u.adminProfile ap
            WHERE
                LOWER(u.email) LIKE LOWER(CONCAT('%', :key, '%'))
                OR LOWER(cp.fullName) LIKE LOWER(CONCAT('%', :key, '%'))
                OR LOWER(mp.fullName) LIKE LOWER(CONCAT('%', :key, '%'))
                OR LOWER(ap.fullName) LIKE LOWER(CONCAT('%', :key, '%'))
            """)
    Page<User> searchUsers(String key, Pageable pageable);
}