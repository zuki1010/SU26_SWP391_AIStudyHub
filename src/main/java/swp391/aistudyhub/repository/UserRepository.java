package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;


import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findUserById(UUID id);

    Optional<User> findByEmailVerificationToken(String token);

    Page<User> findByEmailContainingIgnoreCaseOrCustomerProfileFullNameContainingIgnoreCase(
            String emailKeyword,
            String fullNameKeyword,
            Pageable pageable
    );

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
    Page<User> searchUsers(@Param("key") String key, Pageable pageable);

    @Query("""
            SELECT
                u.email AS email,
                u.accountStatus AS accountStatus,
                u.createdAt AS createdAt,
                cp.fullName AS customerProfileFullName,
                mp.fullName AS moderatorProfileFullName,
                ap.fullName AS adminProfileFullName,
                u.role AS role,
                u.id AS id
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
    Page<UserAccountResponse> searchCustomers(
            @Param("key") String key,
            Pageable pageable
    );

    @Query("""
            SELECT
                u.email AS email,
                u.accountStatus AS accountStatus,
                u.createdAt AS createdAt,
                cp.fullName AS customerProfileFullName,
                mp.fullName AS moderatorProfileFullName,
                ap.fullName AS adminProfileFullName,
                u.role AS role,
                u.id AS id,
                mb.id AS memberId
            FROM User u
            LEFT JOIN u.customerProfile cp
            LEFT JOIN u.moderatorProfile mp
            LEFT JOIN u.adminProfile ap
            LEFT JOIN UserMemberSubscription mb ON u.id = mb.id
            """)
    Page<UserAccountResponse> findBy(Pageable pageable);

    @Query("""
            SELECT
                u.email AS email,
                u.accountStatus AS accountStatus,
                u.createdAt AS createdAt,
                cp.fullName AS customerProfileFullName,
                mp.fullName AS moderatorProfileFullName,
                ap.fullName AS adminProfileFullName,
                u.role AS role
            FROM User u
            LEFT JOIN u.customerProfile cp
            LEFT JOIN u.moderatorProfile mp
            LEFT JOIN u.adminProfile ap
            WHERE u.id = :id
            """)
    UserAccountResponse findProjectedById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountStatus = :status WHERE u.id = :id")
    int updateUserStatus(
            @Param("id") UUID id,
            @Param("status") AccountStatus status
    );

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :id")
    int updateUserRole(
            @Param("id") UUID id,
            @Param("role") UserRole role
    );
}